package com.ibtrader.application;

import com.ibtrader.domain.engine.DecisionEngine;
import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.engine.OrderPlanningEngine;
import com.ibtrader.domain.engine.PortfolioAnalysisEngine;
import com.ibtrader.domain.engine.RuleEvaluationEngine;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.outbound.ExpressionTreeRepository;
import com.ibtrader.domain.port.outbound.IbCommandOutboxPort;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.PortfolioSnapshotRepository;
import com.ibtrader.domain.port.outbound.StrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The central Application Service orchestrating the entire trading pipeline.
 *
 * Execution flow:
 * 1. Build portfolio snapshot
 * 2. Analyze portfolio
 * 3. Load active strategies
 * 4. Build evaluation context
 * 5. Evaluate strategies (Rule Engine) -> TradeSignals
 * 6. Merge & Resolve conflicts (Decision Engine) -> ValidatedTradeDecisions
 * 7. Sizing & Policy (Order Planning Engine) -> OrderPlans
 * 8. Queue orders to Outbox
 */
@Service
@RequiredArgsConstructor
public class TradingEngineOrchestrator {

    private static final Logger LOG = Logger.getLogger(TradingEngineOrchestrator.class.getName());

    // Domain Engines
    private final PortfolioAnalysisEngine portfolioAnalysisEngine;
    private final RuleEvaluationEngine ruleEvaluationEngine;
    private final DecisionEngine decisionEngine;
    private final OrderPlanningEngine orderPlanningEngine;

    // Outbound Ports
    private final PortfolioRepository portfolioRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final StrategyRepository strategyRepository;
    private final ExpressionTreeRepository expressionTreeRepository;
    private final IbCommandOutboxPort outboxPort;

    /**
     * Executes the main trading loop for a given account.
     * This method is transactional to ensure a clean evaluation boundary.
     *
     * @param accountId The IBKR account identifier (e.g. "DU123456")
     */
    public void executePipeline(String accountId) {
        LOG.info(String.format("Starting Trading Engine Pipeline for account: %s", accountId));
        Instant evaluationTime = Instant.now();

        Portfolio portfolio = fetchPortfolio(accountId);
        if (portfolio == null) return;

        snapshotRepository.save(PortfolioSnapshot.from(portfolio));

        PortfolioAnalysis analysis = portfolioAnalysisEngine.analyze(portfolio, portfolio.getNetLiquidationValue());

        List<TradingStrategy> activeStrategies = strategyRepository.findActiveStrategies();
        if (activeStrategies.isEmpty()) {
            LOG.info("No active strategies found. Pipeline execution completed.");
            return;
        }

        List<TradeSignal> allSignals = evaluateStrategies(activeStrategies, portfolio, analysis, evaluationTime);
        if (allSignals.isEmpty()) return;

        List<ValidatedTradeDecision> validatedDecisions = decisionEngine.processSignals(allSignals, null);
        if (validatedDecisions.isEmpty()) return;

        planAndQueueOrders(validatedDecisions, portfolio, analysis, evaluationTime, accountId);
    }

    private Portfolio fetchPortfolio(String accountId) {
        Optional<Portfolio> portfolioOpt = portfolioRepository.findByAccountId(accountId);
        if (portfolioOpt.isEmpty()) {
            LOG.warning(String.format("No portfolio found for account %s. Aborting pipeline.", accountId));
            return null;
        }
        return portfolioOpt.get();
    }

    private List<TradeSignal> evaluateStrategies(List<TradingStrategy> activeStrategies, Portfolio portfolio, 
                                                 PortfolioAnalysis analysis, Instant evaluationTime) {
        List<TradeSignal> allSignals = new ArrayList<>();
        for (TradingStrategy strategy : activeStrategies) {
            EvaluationContext context = EvaluationContext.builder()
                    .portfolio(portfolio)
                    .portfolioAnalysis(analysis)
                    .strategy(strategy)
                    .evaluationTime(evaluationTime)
                    .build();

            expressionTreeRepository.findByStrategyId(strategy.getId()).ifPresent(rootNode -> {
                List<TradeSignal> signals = ruleEvaluationEngine.evaluate(context, rootNode, new ArrayList<>());
                allSignals.addAll(signals);
            });
        }
        if (allSignals.isEmpty()) {
            LOG.info("No trade signals generated in this cycle.");
        }
        return allSignals;
    }

    private void planAndQueueOrders(List<ValidatedTradeDecision> validatedDecisions, Portfolio portfolio, 
                                    PortfolioAnalysis analysis, Instant evaluationTime, String accountId) {
        List<OrderPlan> orderPlans = new ArrayList<>();
        for (ValidatedTradeDecision decision : validatedDecisions) {
            EvaluationContext context = EvaluationContext.builder()
                    .portfolio(portfolio)
                    .portfolioAnalysis(analysis)
                    .strategy(strategyRepository.findById(decision.getStrategyId()).orElseThrow())
                    .evaluationTime(evaluationTime)
                    .build();

            orderPlanningEngine.planOrders(List.of(decision), context).forEach(orderPlans::add);
        }

        for (OrderPlan plan : orderPlans) {
            LOG.info(String.format("Queuing Order Plan for Execution: %s %s %s via %s", 
                plan.getSide(), plan.getTargetQuantity(), plan.getSymbol(), plan.getExecutionPolicy()));
            outboxPort.queueOrderPlan(plan); 
        }

        LOG.info(String.format("Pipeline completed for %s. %d orders queued.", accountId, orderPlans.size()));
    }
}
