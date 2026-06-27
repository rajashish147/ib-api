package com.ibtrader.domain.engine;

import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import lombok.RequiredArgsConstructor;
import java.util.logging.Logger;

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
public class OrderPlanningEngine {

    private static final Logger LOG = Logger.getLogger(OrderPlanningEngine.class.getName());

    /**
     * Converts validated decisions into order plans based on current portfolio context.
     *
     * @param decisions risk-validated trade decisions
     * @param context current evaluation context
     * @return executable order plans
     */
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

                // TODO: Look up actual execution policy tied to strategy version in DB
                // For now, default to IMMEDIATE (Market order)
                String executionPolicy = "IMMEDIATE";
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
        // TODO: Query MarketDataCache for actual price.
        // Returning a placeholder for now to allow compilation and structural completeness.
        return BigDecimal.valueOf(150.0);
    }
}
