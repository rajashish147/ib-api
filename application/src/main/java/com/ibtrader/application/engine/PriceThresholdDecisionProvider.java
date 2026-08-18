package com.ibtrader.application.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.strategy.BasketTarget;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.inbound.provider.DecisionProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PriceThresholdDecisionProvider implements DecisionProvider {

    private static final double CONFIDENCE = 1.0d;
    private static final String QUANTITY_TYPE_SHARES = "SHARES";

    @Override
    public List<TradeSignal> evaluate(EvaluationContext context) {
        if (context == null || context.getStrategy() == null) {
            return List.of();
        }

        TradingStrategy strategy = context.getStrategy();
        if (!strategy.isEnabled() || strategy.getTargets() == null || strategy.getTargets().isEmpty()) {
            return List.of();
        }

        List<TradeSignal> signals = new ArrayList<>();
        for (BasketTarget target : strategy.getTargets()) {
            appendSignalIfThresholdCrossed(context, strategy, target, signals);
        }
        return signals;
    }

    private void appendSignalIfThresholdCrossed(
            EvaluationContext context,
            TradingStrategy strategy,
            BasketTarget target,
            List<TradeSignal> signals) {

        if (target == null || target.getSymbol() == null || target.getQuantity() == null
                || target.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String symbol = target.getSymbol().toUpperCase();
        BigDecimal price = context.getMarketPrice(symbol);
        if (price == null) {
            return;
        }

        if (strategy.getBuyThreshold() != null && price.compareTo(strategy.getBuyThreshold()) <= 0) {
            signals.add(buildSignal(strategy, target, symbol, OrderSide.BUY, price, "Price at or below buy threshold"));
            return;
        }

        if (strategy.getSellThreshold() != null && price.compareTo(strategy.getSellThreshold()) >= 0) {
            signals.add(buildSignal(strategy, target, symbol, OrderSide.SELL, price, "Price at or above sell threshold"));
        }
    }

    private TradeSignal buildSignal(
            TradingStrategy strategy,
            BasketTarget target,
            String symbol,
            OrderSide side,
            BigDecimal price,
            String reason) {

        return TradeSignal.builder()
                .id(UUID.randomUUID())
                .strategyId(strategy.getId())
                .symbol(symbol)
                .action(side)
                .quantityType(QUANTITY_TYPE_SHARES)
                .quantityValue(target.getQuantity())
                .reason(reason + " (" + price.toPlainString() + ")")
                .confidence(CONFIDENCE)
                .generatedAt(Instant.now())
                .build();
    }
}
