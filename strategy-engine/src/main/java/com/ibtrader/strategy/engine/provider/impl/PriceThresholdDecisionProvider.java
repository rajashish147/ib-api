package com.ibtrader.strategy.engine.provider.impl;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.domain.model.strategy.BasketTarget;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.inbound.provider.DecisionProvider;
import com.ibtrader.domain.port.outbound.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DecisionProvider that implements the "buy at X / sell at Y" price-threshold feature.
 *
 * <p>For each basket target in the strategy:</p>
 * <ul>
 *   <li>If {@code buyThreshold} is set and the live price is &le; buyThreshold,
 *       and we do NOT hold a position, a BUY signal is emitted.</li>
 *   <li>If {@code sellThreshold} is set and the live price is &ge; sellThreshold,
 *       and we DO hold a position, a SELL signal is emitted.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceThresholdDecisionProvider implements DecisionProvider {

    private final AssetRepository assetRepository;

    @Override
    public List<TradeSignal> evaluate(EvaluationContext context) {
        List<TradeSignal> signals = new ArrayList<>();
        TradingStrategy strategy = context.getStrategy();

        if (strategy.getBuyThreshold() == null && strategy.getSellThreshold() == null) {
            return signals;
        }
        if (strategy.getTargets() == null || strategy.getTargets().isEmpty()) {
            log.debug("Strategy {} has thresholds but no basket targets.", strategy.getId());
            return signals;
        }

        Portfolio portfolio = context.getPortfolio();
        for (BasketTarget target : strategy.getTargets()) {
            String symbol = target.getSymbol().toUpperCase();
            BigDecimal livePrice = context.getMarketPrice(symbol);
            if (livePrice == null) {
                log.warn("Strategy {}: no live price for {} -- skipping.", strategy.getId(), symbol);
                continue;
            }
            log.debug("Strategy {}: {} price={} buy<={} sell>={}",
                    strategy.getId(), symbol, livePrice,
                    strategy.getBuyThreshold(), strategy.getSellThreshold());

            boolean holding = isHoldingPosition(symbol, portfolio);
            evaluateBuy(strategy, symbol, livePrice, target.getQuantity(), holding, signals);
            evaluateSell(strategy, symbol, livePrice, target.getQuantity(), holding, signals);
        }
        return signals;
    }

    private boolean isHoldingPosition(String symbol, Portfolio portfolio) {
        return assetRepository.findBySymbol(symbol)
                .flatMap(asset -> portfolio.findPosition(asset.getId()))
                .map(Position::getQuantity)
                .map(qty -> new BigDecimal(qty.toString()).compareTo(BigDecimal.ZERO) > 0)
                .orElse(false);
    }

    private void evaluateBuy(TradingStrategy strategy, String symbol, BigDecimal price,
                              BigDecimal qty, boolean holding, List<TradeSignal> signals) {
        if (strategy.getBuyThreshold() == null
                || price.compareTo(strategy.getBuyThreshold()) > 0
                || holding) {
            return;
        }
        log.info("Strategy {}: BUY {} price={} <= threshold={}",
                strategy.getId(), symbol, price, strategy.getBuyThreshold());
        signals.add(buildSignal(strategy.getId(), symbol, OrderSide.BUY, qty,
                String.format("Price %s <= buyThreshold %s", price, strategy.getBuyThreshold())));
    }

    private void evaluateSell(TradingStrategy strategy, String symbol, BigDecimal price,
                               BigDecimal qty, boolean holding, List<TradeSignal> signals) {
        if (strategy.getSellThreshold() == null
                || price.compareTo(strategy.getSellThreshold()) < 0
                || !holding) {
            return;
        }
        log.info("Strategy {}: SELL {} price={} >= threshold={}",
                strategy.getId(), symbol, price, strategy.getSellThreshold());
        signals.add(buildSignal(strategy.getId(), symbol, OrderSide.SELL, qty,
                String.format("Price %s >= sellThreshold %s", price, strategy.getSellThreshold())));
    }

    private TradeSignal buildSignal(UUID strategyId, String symbol, OrderSide side,
                                    BigDecimal quantity, String reason) {
        return TradeSignal.builder()
                .id(UUID.randomUUID())
                .strategyId(strategyId)
                .symbol(symbol)
                .action(side)
                .quantityType("FIXED")
                .quantityValue(quantity)
                .reason(reason)
                .confidence(1.0)
                .generatedAt(Instant.now())
                .build();
    }
}
