package com.ibtrader.domain.port.inbound.provider;

import java.util.List;

public class DecisionProviderRegistry {

    private final List<DecisionProvider> providers;

    public DecisionProviderRegistry(List<DecisionProvider> providers) {
        this.providers = providers;
    }

    public List<DecisionProvider> getProviders() {
        return providers;
    }

    public List<com.ibtrader.domain.model.strategy.TradeSignal> evaluateProviders(
            com.ibtrader.domain.engine.EvaluationContext context) {
        return providers.stream()
                .flatMap(provider -> provider.evaluate(context).stream())
                .toList();
    }
}
