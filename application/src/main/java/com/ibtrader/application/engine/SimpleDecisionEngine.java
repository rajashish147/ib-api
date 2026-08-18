package com.ibtrader.application.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import com.ibtrader.domain.port.inbound.DecisionEnginePort;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimpleDecisionEngine implements DecisionEnginePort {

    @Override
    public List<ValidatedTradeDecision> evaluateSignals(List<TradeSignal> signals, EvaluationContext context) {
        if (signals == null || signals.isEmpty()) {
            return List.of();
        }

        Map<String, ValidatedTradeDecision> decisions = new LinkedHashMap<>();
        for (TradeSignal signal : signals) {
            if (!isUsable(signal)) {
                continue;
            }
            decisions.putIfAbsent(decisionKey(signal), toDecision(signal));
        }
        return List.copyOf(decisions.values());
    }

    private boolean isUsable(TradeSignal signal) {
        return signal != null
                && signal.getStrategyId() != null
                && signal.getSymbol() != null
                && signal.getAction() != null
                && signal.getQuantityValue() != null
                && signal.getQuantityValue().signum() > 0;
    }

    private String decisionKey(TradeSignal signal) {
        return signal.getStrategyId() + "|" + signal.getSymbol().toUpperCase() + "|" + signal.getAction();
    }

    private ValidatedTradeDecision toDecision(TradeSignal signal) {
        return ValidatedTradeDecision.builder()
                .id(UUID.randomUUID())
                .sourceSignalId(signal.getId())
                .strategyId(signal.getStrategyId())
                .symbol(signal.getSymbol().toUpperCase())
                .action(signal.getAction())
                .quantityType(signal.getQuantityType())
                .quantityValue(signal.getQuantityValue())
                .decisionTime(Instant.now())
                .build();
    }
}
