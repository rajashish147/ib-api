package com.ibtrader.application.pipeline;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.strategy.OrderPlan;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.model.strategy.ValidatedTradeDecision;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context passed through the trading engine pipeline stages.
 */
@Getter
@Setter
public class PipelineContext {
    private final String accountId;
    private Portfolio portfolio;
    private List<TradingStrategy> activeStrategies = new ArrayList<>();
    
    // Per-strategy state
    private final Map<TradingStrategy, EvaluationContext> evaluationContexts = new HashMap<>();
    private final Map<TradingStrategy, List<TradeSignal>> tradeSignals = new HashMap<>();
    private final Map<TradingStrategy, List<ValidatedTradeDecision>> decisions = new HashMap<>();
    private final Map<TradingStrategy, List<OrderPlan>> orderPlans = new HashMap<>();

    public PipelineContext(String accountId) {
        this.accountId = accountId;
    }
}
