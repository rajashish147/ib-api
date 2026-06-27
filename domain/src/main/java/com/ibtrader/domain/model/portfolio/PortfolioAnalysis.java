package com.ibtrader.domain.model.portfolio;

import com.ibtrader.domain.model.asset.AssetClass;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.common.Percentage;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable record containing the result of a comprehensive portfolio analysis.
 * Produced by the PortfolioAnalysisEngine and passed into the EvaluationContext
 * for rule processing.
 */
@Getter
@Builder
@ToString
public class PortfolioAnalysis {

    // ── High-level values ─────────────────────────────────────────────────────
    private final Money totalPortfolioValue;
    private final Money netLiquidationValue;
    private final Money cashBalance;
    private final Money availableBuyingPower;
    private final Percentage marginUsage;
    private final Percentage availableCashPercentage;
    
    // ── Profit & Loss ─────────────────────────────────────────────────────────
    private final Money unrealizedPnL;
    private final Money realizedPnL;
    private final Money dailyPnL;
    private final Money totalPnL;
    
    // ── Allocations & Exposures ───────────────────────────────────────────────
    private final Map<AssetClass, Percentage> assetClassAllocation;
    private final Map<String, Percentage> sectorAllocation;
    private final Map<UUID, Percentage> positionAllocation;
    
    private final Map<AssetClass, Money> assetClassExposure;
    private final Map<String, Money> sectorExposure;
    
    // ── Risk & Concentration ──────────────────────────────────────────────────
    private final int numberOfOpenPositions;
    private final List<Position> largestHoldings;
    
    private final Percentage concentrationRisk; // Largest single position %
    private final BigDecimal leverage; // Total Position Value / NLV
    private final BigDecimal riskScore; // Calculated based on concentration & leverage
    
    // ── Historical / Drawdown ─────────────────────────────────────────────────
    private final Money portfolioPeakValue;
    private final Percentage drawdownFromPeak;
    
    // ── Metadata ──────────────────────────────────────────────────────────────
    private final Instant analyzedAt;
}
