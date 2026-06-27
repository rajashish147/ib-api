package com.ibtrader.domain.engine;

import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import lombok.RequiredArgsConstructor;
import java.util.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Receives raw TradeSignals from the RuleEvaluationEngine and processes them.
 * Resolves conflicts (e.g., competing buy/sell on same asset), enforces strategy
 * cooldowns, and filters out duplicates.
 */
@RequiredArgsConstructor
public class DecisionEngine {

    private static final Logger LOG = Logger.getLogger(DecisionEngine.class.getName());

    /**
     * Processes raw signals into validated decisions.
     *
     * @param signals raw signals from rule evaluation
     * @param context current evaluation context
     * @return a list of validated decisions ready for risk checks
     */
    public List<ValidatedTradeDecision> processSignals(List<TradeSignal> signals, EvaluationContext context) {
        List<ValidatedTradeDecision> decisions = new ArrayList<>();
        if (signals == null || signals.isEmpty()) {
            return decisions;
        }

        // 1. Group by symbol to detect conflicting signals on the same asset
        Map<String, List<TradeSignal>> signalsBySymbol = signals.stream()
                .collect(Collectors.groupingBy(TradeSignal::getSymbol));

        for (Map.Entry<String, List<TradeSignal>> entry : signalsBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<TradeSignal> symbolSignals = entry.getValue();

            // Simple conflict resolution: if we have both BUY and SELL for the same symbol in one cycle,
            // we discard both to prevent wash trading loops.
            boolean hasBuy = symbolSignals.stream().anyMatch(s -> s.getAction().isBuy());
            boolean hasSell = symbolSignals.stream().anyMatch(s -> s.getAction().isSell());

            if (hasBuy && hasSell) {
                LOG.warning(String.format("Conflicting BUY and SELL signals generated for symbol %s. Discarding both.", symbol));
                continue;
            }

            // Deduplication: If multiple identical signals exist for the same symbol and action, 
            // pick the one with highest confidence (or just the first one if confidence is equal)
            TradeSignal bestSignal = symbolSignals.stream()
                    .max((s1, s2) -> Double.compare(s1.getConfidence(), s2.getConfidence()))
                    .orElse(symbolSignals.get(0));

            // TODO: In a real system, query the database or cache to enforce Cooldown period
            // e.g., if (lastTradeTime(symbol, strategy) < cooldown) -> discard

            ValidatedTradeDecision decision = ValidatedTradeDecision.builder()
                    .id(UUID.randomUUID())
                    .sourceSignalId(bestSignal.getId())
                    .strategyId(bestSignal.getStrategyId())
                    .symbol(bestSignal.getSymbol())
                    .action(bestSignal.getAction())
                    .quantityType(bestSignal.getQuantityType())
                    .quantityValue(bestSignal.getQuantityValue())
                    .decisionTime(Instant.now())
                    .build();

            decisions.add(decision);
            LOG.info(String.format("Validated Trade Decision generated: %s %s for strategy %s", 
                decision.getAction(), decision.getSymbol(), decision.getStrategyId()));
        }

        return decisions;
    }
}
