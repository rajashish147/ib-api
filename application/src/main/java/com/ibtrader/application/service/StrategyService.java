package com.ibtrader.application.service;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.event.RebalancePlanGeneratedEvent;
import com.ibtrader.domain.exception.DomainException;
import com.ibtrader.domain.exception.OrderNotFoundException;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.RebalancePlan;
import com.ibtrader.domain.model.strategy.StrategyMode;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.model.strategy.TriggerType;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.DecisionEnginePort;
import com.ibtrader.domain.port.inbound.EvaluateStrategyUseCase;
import com.ibtrader.domain.port.inbound.EvaluationContextFactory;
import com.ibtrader.domain.port.inbound.ExecuteRebalancePlanUseCase;
import com.ibtrader.domain.port.inbound.GenerateRebalancePlanUseCase;
import com.ibtrader.domain.port.inbound.ManageStrategyUseCase;
import com.ibtrader.domain.port.inbound.OrderPlanningPort;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import com.ibtrader.domain.port.outbound.IbCommandOutboxPort;
import com.ibtrader.domain.port.outbound.RebalancePlanRepository;
import com.ibtrader.domain.port.outbound.StrategyRepository;
import com.ibtrader.domain.port.inbound.provider.DecisionProvider;
import com.ibtrader.domain.port.inbound.provider.DecisionProviderRegistry;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.strategy.RebalancePlanItem;
import com.ibtrader.domain.model.common.Percentage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service coordinating trading strategy evaluation and rebalance plan execution.
 * Acts as the primary entry point for the strategy scheduler and admin API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StrategyService implements EvaluateStrategyUseCase, 
        ExecuteRebalancePlanUseCase, GenerateRebalancePlanUseCase, ManageStrategyUseCase {

    private final StrategyRepository strategyRepository;
    private final RebalancePlanRepository rebalancePlanRepository;
    private final EvaluationContextFactory evaluationContextFactory;
    private final DecisionProviderRegistry decisionProviderRegistry;
    private final DecisionEnginePort decisionEnginePort;
    private final OrderPlanningPort orderPlanningPort;
    private final IbCommandOutboxPort outboxPort;
    private final DomainEventPublisher domainEventPublisher;
    private final AssetRepository assetRepository;

    @Override
    public void execute(EvaluateStrategyUseCase.Command command) {
        log.info("Evaluating strategy {} for account {}", command.strategyId(), command.accountId());
        
        TradingStrategy strategy = strategyRepository.findById(command.strategyId())
                .orElseThrow(() -> new IllegalArgumentException("Strategy not found"));

        if (!strategy.isEnabled()) {
            throw new StrategyDisabledException("Strategy " + strategy.getId() + " is disabled.");
        }

        GenerateRebalancePlanUseCase.Command genCommand = new GenerateRebalancePlanUseCase.Command(
                command.strategyId(), command.accountId(), TriggerType.SCHEDULED);
        execute(genCommand);
    }

    @Override
    public void execute(ExecuteRebalancePlanUseCase.Command command) {
        log.info("Executing rebalance plan {}", command.planId());
        
        RebalancePlan plan = rebalancePlanRepository.findById(command.planId())
                .orElseThrow(() -> OrderNotFoundException.byId(command.planId()));

        if (!plan.isExecutable()) {
            log.warn("Rebalance plan {} is not executable. Current status: {}", 
                     plan.getId(), plan.getStatus());
            return;
        }

        plan.markExecuting();
        rebalancePlanRepository.save(plan);

        log.info("Queueing orders to Outbox for plan {}", plan.getId());
        for (RebalancePlanItem item : plan.getItems()) {
            if (item.requiresTrade()) {
                OrderPlan orderPlan = OrderPlan.builder()
                        .id(UUID.randomUUID())
                        .strategyId(plan.getStrategyId())
                        .symbol(item.getSymbol())
                        .side(item.getSide())
                        .targetQuantity(item.getQuantityDelta().abs())
                        .build();
                outboxPort.queueOrderPlan(orderPlan);
                item.assignOrder(orderPlan.getId());
            }
        }

        plan.complete();
        rebalancePlanRepository.save(plan);
        log.info("Rebalance plan {} executed and completed", plan.getId());
    }

    @Override
    public RebalancePlan execute(GenerateRebalancePlanUseCase.Command command) {
        log.info("Generating rebalance plan for strategy {}, account {}, trigger {}", 
                 command.strategyId(), command.accountId(), command.triggerType());

        TradingStrategy strategy = strategyRepository.findById(command.strategyId())
                .orElseThrow(() -> new IllegalArgumentException("Strategy not found"));

        if (!strategy.isEnabled()) {
            throw new StrategyDisabledException("Strategy " + strategy.getId() + " is disabled.");
        }

        StrategyMode mode;
        try {
            mode = StrategyMode.valueOf(strategy.getExecutionMode().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            log.warn("Invalid execution mode {}, defaulting to FULL_REBALANCE", 
                     strategy.getExecutionMode());
            mode = StrategyMode.FULL_REBALANCE;
        }

        EvaluationContext context = evaluationContextFactory.create(strategy, command.accountId());
        
        List<OrderPlan> orderPlans = generateOrderPlans(context);
        return buildAndSaveRebalancePlan(command, strategy, mode, orderPlans);
    }
    
    private List<OrderPlan> generateOrderPlans(EvaluationContext context) {
        List<TradeSignal> mergedSignals = new ArrayList<>();
        for (DecisionProvider provider : decisionProviderRegistry.getProviders()) {
            List<TradeSignal> signals = provider.evaluate(context);
            if (signals != null) {
                mergedSignals.addAll(signals);
            }
        }
        List<ValidatedTradeDecision> decisions = decisionEnginePort.evaluateSignals(
                mergedSignals, context);
        return orderPlanningPort.planOrders(decisions, context);
    }
    
    private RebalancePlan buildAndSaveRebalancePlan(GenerateRebalancePlanUseCase.Command command, 
                                                    TradingStrategy strategy, 
                                                    StrategyMode mode, 
                                                    List<OrderPlan> orderPlans) {
        Money totalBuy = Money.zeroUsd();
        Money totalSell = Money.zeroUsd();
        
        for (OrderPlan op : orderPlans) {
            BigDecimal price = op.getLimitPrice() != null ? op.getLimitPrice().getAmount() : BigDecimal.ZERO;
            BigDecimal value = op.getTargetQuantity().multiply(price);
            if (op.getSide().isBuy()) {
                totalBuy = totalBuy.add(Money.usd(value));
            } else {
                totalSell = totalSell.add(Money.usd(value));
            }
        }

        RebalancePlan plan = RebalancePlan.create(strategy.getId(), command.triggerType(), mode, totalBuy, totalSell);

        for (OrderPlan op : orderPlans) {
            Asset asset = assetRepository.findBySymbol(op.getSymbol()).orElse(null);
            if (asset != null) {
                BigDecimal currentQty = BigDecimal.ZERO; // Placeholder for now
                BigDecimal targetQty = op.getSide().isBuy() ? 
                        currentQty.add(op.getTargetQuantity()) : currentQty.subtract(op.getTargetQuantity());
                RebalancePlanItem item = RebalancePlanItem.create(
                        plan.getId(),
                        asset.getId(),
                        op.getSymbol(),
                        Percentage.of(BigDecimal.ZERO), // Placeholder
                        Percentage.of(BigDecimal.ZERO), // Placeholder
                        currentQty,
                        targetQty,
                        op.getLimitPrice() != null ? 
                                Money.of(op.getLimitPrice().getAmount(), "USD") : Money.of(BigDecimal.ZERO, "USD")
                );
                plan.addItem(item);
            }
        }

        plan.approve();
        RebalancePlan savedPlan = rebalancePlanRepository.save(plan);

        RebalancePlanGeneratedEvent event = RebalancePlanGeneratedEvent.builder()
                .planId(savedPlan.getId())
                .strategyId(savedPlan.getStrategyId())
                .mode(savedPlan.getMode())
                .triggerType(savedPlan.getTriggerType())
                .nlvAtTrigger(BigDecimal.ZERO)
                .itemCount(savedPlan.getItems().size())
                .totalBuyValue(totalBuy.getAmount())
                .totalSellValue(totalSell.getAmount())
                .sequenceNumber(System.currentTimeMillis())
                .build();

        domainEventPublisher.publish(event);

        return savedPlan;
    }
    
    @Override
    public List<TradingStrategy> getActiveStrategies() {
        return strategyRepository.findActiveStrategies();
    }
    
    @Override
    public List<TradingStrategy> getAllStrategies() {
        return strategyRepository.findAll();
    }
    
    @Override
    public TradingStrategy getStrategyById(UUID id) {
        return strategyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Strategy not found"));
    }
    
    @Override
    public TradingStrategy createStrategy(TradingStrategy strategy) {
        return strategyRepository.save(strategy);
    }
    
    @Override
    public TradingStrategy updateStrategy(UUID id, TradingStrategy strategy) {
        return strategyRepository.save(strategy);
    }
    
    @Override
    public TradingStrategy toggleStrategy(UUID id, boolean enabled) {
        TradingStrategy strategy = getStrategyById(id);
        TradingStrategy updated = strategy.toBuilder().enabled(enabled).build();
        return strategyRepository.save(updated);
    }
    
    @Override
    public void deleteStrategy(UUID id) {
        strategyRepository.deleteById(id);
    }

    static class StrategyDisabledException extends DomainException {
        StrategyDisabledException(String message) {
            super("STRATEGY_DISABLED", message);
        }
    }
}
