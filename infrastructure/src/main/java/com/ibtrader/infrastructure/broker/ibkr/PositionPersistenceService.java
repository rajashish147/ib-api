package com.ibtrader.infrastructure.broker.ibkr;

import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.infrastructure.persistence.entity.PositionEntity;
import com.ibtrader.infrastructure.persistence.repository.PositionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Handles transactional persistence of IB position callbacks.
 *
 * <p>Extracted from {@link IbConnectionManager} so that Spring AOP can correctly
 * proxy the {@code @Transactional} boundary.  Private methods on the same class
 * bypass the proxy and do NOT participate in container-managed transactions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionPersistenceService {

    private final PositionJpaRepository positionJpaRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;

    /**
     * Upserts a single IB position callback into the positions table.
     *
     * <ul>
     *   <li>Quantity {@code 0} → DELETE (position fully closed)</li>
     *   <li>Existing row → UPDATE (preserves {@code @Version} for optimistic locking)</li>
     *   <li>No existing row → INSERT via {@code @GeneratedValue} UUID</li>
     * </ul>
     *
     * @param account  IB account string (e.g. {@code "DUP854695"})
     * @param symbol   ticker symbol (e.g. {@code "META"})
     * @param currency ISO currency code (e.g. {@code "USD"})
     * @param quantity position quantity (negative for short)
     * @param avgCost  average cost per share
     */
    @Transactional
    public void upsertPosition(String account, String symbol, String currency,
                               double quantity, double avgCost) {
        if (symbol == null || symbol.isBlank()) {
            log.warn("Received position callback with blank symbol — skipping");
            return;
        }

        var portfolioOpt = portfolioRepository.findByAccountId(account);
        if (portfolioOpt.isEmpty()) {
            log.warn("No portfolio found for account {} — cannot persist position {}", account, symbol);
            return;
        }
        UUID portfolioId = portfolioOpt.get().getId();

        var assetOpt = assetRepository.findBySymbol(symbol.toUpperCase());
        if (assetOpt.isEmpty()) {
            log.warn("Position received for unregistered asset '{}' — add it to the assets table to track it", symbol);
            return;
        }
        UUID assetId = assetOpt.get().getId();

        BigDecimal qty = BigDecimal.valueOf(quantity).stripTrailingZeros();
        String cur = (currency != null && !currency.isBlank()) ? currency : "USD";

        // IB sends qty=0 when a position is fully closed → remove from DB
        if (qty.compareTo(BigDecimal.ZERO) == 0) {
            positionJpaRepository.deleteByPortfolioIdAndAssetId(portfolioId, assetId);
            log.info("Deleted closed position for {} in portfolio {}", symbol, portfolioId);
            return;
        }

        BigDecimal cost        = BigDecimal.valueOf(avgCost).setScale(4, RoundingMode.HALF_UP);
        BigDecimal marketValue = cost.multiply(qty.abs()).setScale(4, RoundingMode.HALF_UP);

        var existingOpt = positionJpaRepository.findByPortfolioIdAndAssetId(portfolioId, assetId);
        if (existingOpt.isPresent()) {
            // Existing entity carries @Version — JPA will do a correct UPDATE
            PositionEntity entity = existingOpt.get();
            entity.setQuantity(qty);
            entity.setAverageCost(cost);
            entity.setMarketPrice(cost);   // proxy until live tick updates this
            entity.setMarketValue(marketValue);
            entity.setCurrency(cur);
            entity.setUnrealizedPnl(BigDecimal.ZERO);
            entity.setRealizedPnl(BigDecimal.ZERO);
            entity.setLastUpdated(Instant.now());
            positionJpaRepository.save(entity);
            log.info("Updated position: {} x {} @ {} {}", qty, symbol, cost, cur);
        } else {
            // New entity without explicit id — @GeneratedValue assigns UUID
            PositionEntity entity = PositionEntity.builder()
                    .portfolioId(portfolioId)
                    .assetId(assetId)
                    .symbol(symbol.toUpperCase())
                    .quantity(qty)
                    .averageCost(cost)
                    .currency(cur)
                    .marketPrice(cost)
                    .marketValue(marketValue)
                    .unrealizedPnl(BigDecimal.ZERO)
                    .realizedPnl(BigDecimal.ZERO)
                    .lastUpdated(Instant.now())
                    .build();
            positionJpaRepository.save(entity);
            log.info("Created position: {} x {} @ {} {}", qty, symbol, cost, cur);
        }
    }
}
