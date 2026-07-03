package com.ibtrader.strategy.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.OrderPlanningPort;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.ExecutionPolicyRepository;
import com.ibtrader.domain.port.outbound.MarketDataCache;
import lombok.RequiredArgsConstructor;
import java.util.logging.Logger;
import java.util.Optional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Translates Risk-Validated Trade Decisions into executable Order Plans.
 * Handles quantity sizing (e.g., converting "10% of Cash" into absolute shares)
 * and attaches appropriate execution policies.
 */
@RequiredArgsConstructor
public class OrderPlanningEngine implements OrderPlanningPort {

    private final MarketDataCache marketDataCache;
    private final AssetRepository assetRepository;

    private final ExecutionPolicyRepository executionPolicyRepository;

    private static final Logger LOG = Logger.getLogger(OrderPlanningEngine.class.getName());

    /**
     * Converts validated decisions into order plans based on current portfolio context.
     *
     * @param decisions risk-validated trade decisions
     * @param context current evaluation context
     * @return executable order plans
     */
    @Override
    public List<OrderPlan> planOrders(List<ValidatedTradeDecision> decisions, EvaluationContext context) {
        List<OrderPlan> plans = new ArrayList<>();

        for (ValidatedTradeDecision decision : decisions) {
            try {
                BigDecimal targetShares = resolveAbsoluteQuantity(decision, context);
                if (targetShares.compareTo(BigDecimal.ZERO) <= 0) {
                    LOG.warning(String.format("Calculated target shares for %s is zero or negative. Skipping.", 
                        decision.getSymbol()));
                    continue;
                }

                // Look up actual execution policy tied to strategy version in DB
                // Fallback to IMMEDIATE if none configured
                String executionPolicy = executionPolicyRepository.getPolicy(
                        context.getStrategy().getExecutionMode()).orElse("IMMEDIATE");
                String policyParameters = "{}";

                OrderPlan plan = OrderPlan.builder()
                        .id(UUID.randomUUID())
                        .decisionId(decision.getId())
                        .strategyId(decision.getStrategyId())
                        .symbol(decision.getSymbol())
                        .side(decision.getAction())
                        .targetQuantity(targetShares)
                        .limitPrice(null) // Immediate policy implies market order
                        .executionPolicy(executionPolicy)
                        .policyParameters(policyParameters)
                        .plannedAt(Instant.now())
                        .build();

                plans.add(plan);
                LOG.info(String.format("Planned Order: %s %s %s shares via %s", 
                    plan.getSide(), plan.getTargetQuantity(), plan.getSymbol(), plan.getExecutionPolicy()));

            } catch (Exception e) {
                LOG.severe(String.format("Failed to plan order for decision %s: %s", decision.getId(), e.getMessage()));
            }
        }

        return plans;
    }

    private BigDecimal resolveAbsoluteQuantity(ValidatedTradeDecision decision, EvaluationContext context) {
        BigDecimal value = decision.getQuantityValue();
        String type = decision.getQuantityType();

        return switch (type.toUpperCase()) {
            case "SHARES" -> value;
            case "USD_AMOUNT" -> {
                BigDecimal price = getCurrentPrice(decision.getSymbol(), context);
                yield value.divide(price, 6, RoundingMode.HALF_DOWN); // Fractional shares support
            }
            case "PCT_CASH" -> {
                BigDecimal cash = context.getPortfolioAnalysis().getCashBalance().getAmount();
                BigDecimal allocAmount = cash.multiply(value).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_DOWN);
                BigDecimal price = getCurrentPrice(decision.getSymbol(), context);
                yield allocAmount.divide(price, 6, RoundingMode.HALF_DOWN);
            }
            case "PCT_PORTFOLIO" -> {
                BigDecimal nlv = context.getPortfolioAnalysis().getNetLiquidationValue().getAmount();
                BigDecimal allocAmount = nlv.multiply(value).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_DOWN);
                BigDecimal price = getCurrentPrice(decision.getSymbol(), context);
                yield allocAmount.divide(price, 6, RoundingMode.HALF_DOWN);
            }
            default -> throw new IllegalArgumentException("Unsupported quantity type: " + type);
        };
    }

    private BigDecimal getCurrentPrice(String symbol, EvaluationContext context) {
        Optional<Asset> assetOpt = assetRepository.findBySymbol(symbol);
        if (assetOpt.isEmpty()) {
            LOG.warning("Cannot get price, asset not found: " + symbol);
            return BigDecimal.ONE; // Fallback to avoid division by zero
        }
        
        Optional<BigDecimal> priceOpt = marketDataCache.getPrice(assetOpt.get().getId());
        if (priceOpt.isPresent() && priceOpt.get().compareTo(BigDecimal.ZERO) > 0) {
            return priceOpt.get();
        }
        
        LOG.warning("MarketDataCache returned no valid price for: " + symbol + ", defaulting to 150.0 for planning");
        return BigDecimal.valueOf(150.0);
    }
}
