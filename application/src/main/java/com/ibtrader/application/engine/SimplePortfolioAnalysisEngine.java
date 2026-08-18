package com.ibtrader.application.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.asset.AssetClass;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.common.Percentage;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.domain.port.inbound.PortfolioAnalysisPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SimplePortfolioAnalysisEngine implements PortfolioAnalysisPort {

    @Override
    public PortfolioAnalysis analyzePortfolio(EvaluationContext context) {
        Portfolio portfolio = context != null ? context.getPortfolio() : null;
        if (portfolio == null) {
            return zeroAnalysis();
        }

        Money positionValue = portfolio.totalPositionValue();
        Money nlv = valueOrZero(portfolio.getNetLiquidationValue());
        Money cash = valueOrZero(portfolio.getTotalCashValue());
        Money buyingPower = valueOrZero(portfolio.getBuyingPower());
        Money unrealizedPnl = valueOrZero(portfolio.getUnrealizedPnL());
        Money realizedPnl = valueOrZero(portfolio.getRealizedPnL());
        List<Position> largestHoldings = portfolio.getPositions().stream()
                .sorted(Comparator.comparing(position -> position.getMarketValue().getAmount(), Comparator.reverseOrder()))
                .limit(5)
                .toList();

        return PortfolioAnalysis.builder()
                .totalPortfolioValue(positionValue)
                .netLiquidationValue(nlv)
                .cashBalance(cash)
                .availableBuyingPower(buyingPower)
                .marginUsage(Percentage.zero())
                .availableCashPercentage(cash.percentageOf(nlv.isZero() ? Money.usd(BigDecimal.ONE) : nlv))
                .unrealizedPnL(unrealizedPnl)
                .realizedPnL(realizedPnl)
                .dailyPnL(Money.zeroUsd())
                .totalPnL(unrealizedPnl.add(realizedPnl))
                .assetClassAllocation(Map.of())
                .sectorAllocation(Map.of())
                .positionAllocation(Map.of())
                .assetClassExposure(Map.of())
                .sectorExposure(Map.of())
                .numberOfOpenPositions(portfolio.getPositions().size())
                .largestHoldings(largestHoldings)
                .concentrationRisk(concentrationRisk(largestHoldings, nlv))
                .leverage(leverage(positionValue, nlv))
                .riskScore(BigDecimal.ZERO)
                .portfolioPeakValue(nlv)
                .drawdownFromPeak(Percentage.zero())
                .analyzedAt(Instant.now())
                .build();
    }

    private PortfolioAnalysis zeroAnalysis() {
        return PortfolioAnalysis.builder()
                .totalPortfolioValue(Money.zeroUsd())
                .netLiquidationValue(Money.zeroUsd())
                .cashBalance(Money.zeroUsd())
                .availableBuyingPower(Money.zeroUsd())
                .marginUsage(Percentage.zero())
                .availableCashPercentage(Percentage.zero())
                .unrealizedPnL(Money.zeroUsd())
                .realizedPnL(Money.zeroUsd())
                .dailyPnL(Money.zeroUsd())
                .totalPnL(Money.zeroUsd())
                .assetClassAllocation(Map.<AssetClass, Percentage>of())
                .sectorAllocation(Map.<String, Percentage>of())
                .positionAllocation(Map.<UUID, Percentage>of())
                .assetClassExposure(Map.<AssetClass, Money>of())
                .sectorExposure(Map.<String, Money>of())
                .numberOfOpenPositions(0)
                .largestHoldings(List.of())
                .concentrationRisk(Percentage.zero())
                .leverage(BigDecimal.ZERO)
                .riskScore(BigDecimal.ZERO)
                .portfolioPeakValue(Money.zeroUsd())
                .drawdownFromPeak(Percentage.zero())
                .analyzedAt(Instant.now())
                .build();
    }

    private Money valueOrZero(Money value) {
        return value != null ? value : Money.zeroUsd();
    }

    private Percentage concentrationRisk(List<Position> positions, Money nlv) {
        if (positions.isEmpty() || nlv == null || nlv.isZero()) {
            return Percentage.zero();
        }
        return positions.get(0).getMarketValue().percentageOf(nlv);
    }

    private BigDecimal leverage(Money positionValue, Money nlv) {
        if (positionValue == null || nlv == null || nlv.isZero()) {
            return BigDecimal.ZERO;
        }
        return positionValue.getAmount().divide(nlv.getAmount(), 6, RoundingMode.HALF_UP);
    }
}
