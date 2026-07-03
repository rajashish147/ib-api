package com.ibtrader.strategy.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.asset.AssetClass;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.common.Percentage;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioAnalysis;
import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.domain.port.inbound.PortfolioAnalysisPort;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.MarketDataCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioAnalysisEngine implements PortfolioAnalysisPort {

    private final AssetRepository assetRepository;
    private final MarketDataCache marketDataCache;

    @Override
    public PortfolioAnalysis analyzePortfolio(EvaluationContext context) {
        return analyze(context.getPortfolio(), null);
    }

    /**
     * Performs a comprehensive analysis on the provided portfolio, looking up
     * asset metadata (sectors, classes) as needed.
     *
     * @param portfolio the current state of the portfolio
     * @param peakNlv   the historical peak net liquidation value (for drawdown)
     * @return a point-in-time analysis result
     */
    public PortfolioAnalysis analyze(Portfolio portfolio, Money peakNlv) {
        Money nlv = portfolio.getNetLiquidationValue();
        Money cash = portfolio.getTotalCashValue();
        
        // Ensure NLV is not zero to prevent division by zero
        boolean hasNlv = nlv != null && nlv.getAmount().compareTo(BigDecimal.ZERO) > 0;
        BigDecimal nlvAmount = hasNlv ? nlv.getAmount() : BigDecimal.ONE;
        String currency = hasNlv ? nlv.getCurrency() : "USD";

        // Allocations & Exposures
        Map<AssetClass, Percentage> assetClassAllocation = new EnumMap<>(AssetClass.class);
        Map<String, Percentage> sectorAllocation = new HashMap<>();
        Map<UUID, Percentage> positionAllocation = new HashMap<>();
        
        Map<AssetClass, Money> assetClassExposure = new EnumMap<>(AssetClass.class);
        Map<String, Money> sectorExposure = new HashMap<>();
        
        Percentage maxConcentration = Percentage.of(BigDecimal.ZERO);
        Money totalPositionValue = Money.of(BigDecimal.ZERO, currency);

        for (Position position : portfolio.getPositions()) {
            if (position.isClosed()) continue;

            Money mv = position.getMarketValue();
            Optional<BigDecimal> latestPrice = marketDataCache.getPrice(position.getAssetId());
            if (latestPrice.isPresent()) {
                BigDecimal newValue = latestPrice.get().multiply(position.getQuantity().abs());
                mv = Money.of(newValue, mv.getCurrency());
            }

            totalPositionValue = totalPositionValue.add(mv);
            
            Percentage alloc = calculateAllocation(mv, nlvAmount);
            positionAllocation.put(position.getAssetId(), alloc);
            
            if (alloc.getValue().compareTo(maxConcentration.getValue()) > 0) {
                maxConcentration = alloc;
            }

            accumulateExposures(position, mv, assetClassExposure, sectorExposure);
        }
        
        convertExposuresToAllocations(assetClassExposure, sectorExposure, nlvAmount, 
                                      assetClassAllocation, sectorAllocation);

        // Largest Holdings (Sorted by market value desc)
        List<Position> largestHoldings = portfolio.getPositions().stream()
                .filter(p -> !p.isClosed())
                .sorted(Comparator.comparing(p -> ((Position) p).getMarketValue().getAmount()).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // Leverage & Margin
        BigDecimal leverage = totalPositionValue.getAmount().divide(nlvAmount, 4, RoundingMode.HALF_UP);
        BigDecimal marginPct = portfolio.getMaintenanceMargin().getAmount()
                .divide(nlvAmount, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        Percentage marginUsage = Percentage.of(marginPct);

        // Drawdown
        Percentage drawdown = Percentage.of(BigDecimal.ZERO);
        if (peakNlv != null && peakNlv.getAmount().compareTo(nlv.getAmount()) > 0) {
            BigDecimal drop = peakNlv.getAmount().subtract(nlv.getAmount());
            BigDecimal ddPct = drop.divide(peakNlv.getAmount(), 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            drawdown = Percentage.of(ddPct);
        }

        // Risk Score (0-100 placeholder heuristic)
        BigDecimal riskScore = maxConcentration.getValue()
                .add(leverage.multiply(BigDecimal.valueOf(20)))
                .min(BigDecimal.valueOf(100));

        return buildAnalysis(portfolio, nlv, cash, nlvAmount, currency, peakNlv, 
                assetClassAllocation, sectorAllocation, positionAllocation, 
                assetClassExposure, sectorExposure, maxConcentration, 
                leverage, marginUsage, drawdown, riskScore, largestHoldings);
    }
    
    private void accumulateExposures(Position position, Money mv, 
                                     Map<AssetClass, Money> assetClassExposure, 
                                     Map<String, Money> sectorExposure) {
        Optional<Asset> assetOpt = assetRepository.findById(position.getAssetId());
        if (assetOpt.isPresent()) {
            Asset asset = assetOpt.get();
            assetClassExposure.merge(asset.getAssetClass(), mv, Money::add);
            String sector = "UNKNOWN"; // Placeholder
            sectorExposure.merge(sector, mv, Money::add);
        }
    }
    
    private Percentage calculateAllocation(Money marketValue, BigDecimal nlvAmount) {
        BigDecimal allocPct = marketValue.getAmount().divide(nlvAmount, 6, RoundingMode.HALF_UP)
                                  .multiply(BigDecimal.valueOf(100));
        return Percentage.of(allocPct);
    }

    private void convertExposuresToAllocations(Map<AssetClass, Money> assetClassExposure,
                                               Map<String, Money> sectorExposure,
                                               BigDecimal nlvAmount,
                                               Map<AssetClass, Percentage> assetClassAllocation,
                                               Map<String, Percentage> sectorAllocation) {
        for (Map.Entry<AssetClass, Money> entry : assetClassExposure.entrySet()) {
            BigDecimal alloc = entry.getValue().getAmount()
                    .divide(nlvAmount, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            assetClassAllocation.put(entry.getKey(), Percentage.of(alloc));
        }
        for (Map.Entry<String, Money> entry : sectorExposure.entrySet()) {
            BigDecimal alloc = entry.getValue().getAmount()
                    .divide(nlvAmount, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            sectorAllocation.put(entry.getKey(), Percentage.of(alloc));
        }
    }

    private PortfolioAnalysis buildAnalysis(Portfolio portfolio, Money nlv, Money cash, 
                                            BigDecimal nlvAmount, String currency, Money peakNlv,
                                            Map<AssetClass, Percentage> assetClassAllocation, 
                                            Map<String, Percentage> sectorAllocation, 
                                            Map<UUID, Percentage> positionAllocation, 
                                            Map<AssetClass, Money> assetClassExposure, 
                                            Map<String, Money> sectorExposure, 
                                            Percentage maxConcentration, BigDecimal leverage, 
                                            Percentage marginUsage, Percentage drawdown, 
                                            BigDecimal riskScore, List<Position> largestHoldings) {
        return PortfolioAnalysis.builder()
                .totalPortfolioValue(nlv)
                .netLiquidationValue(nlv)
                .cashBalance(cash)
                .availableBuyingPower(portfolio.getBuyingPower())
                .marginUsage(marginUsage)
                .availableCashPercentage(portfolio.cashPercentage())
                .unrealizedPnL(portfolio.getUnrealizedPnL())
                .realizedPnL(portfolio.getRealizedPnL())
                .dailyPnL(Money.of(BigDecimal.ZERO, currency))
                .totalPnL(portfolio.getRealizedPnL().add(portfolio.getUnrealizedPnL()))
                .assetClassAllocation(assetClassAllocation)
                .sectorAllocation(sectorAllocation)
                .positionAllocation(positionAllocation)
                .assetClassExposure(assetClassExposure)
                .sectorExposure(sectorExposure)
                .numberOfOpenPositions((int) portfolio.getPositions().stream().filter(p -> !p.isClosed()).count())
                .largestHoldings(largestHoldings)
                .concentrationRisk(maxConcentration)
                .leverage(leverage)
                .riskScore(riskScore)
                .portfolioPeakValue(peakNlv != null ? peakNlv : nlv)
                .drawdownFromPeak(drawdown)
                .analyzedAt(Instant.now())
                .build();
    }
}
