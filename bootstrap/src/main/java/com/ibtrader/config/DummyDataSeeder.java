package com.ibtrader.config;

import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.outbound.StrategyRepository;
import com.ibtrader.infrastructure.persistence.entity.PortfolioSnapshotEntity;
import com.ibtrader.infrastructure.persistence.repository.PortfolioSnapshotJpaRepository;
import com.ibtrader.infrastructure.persistence.repository.TradingStrategyJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.context.annotation.Profile;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev") // Only runs in development mode — never in production
public class DummyDataSeeder implements CommandLineRunner {


    private final PortfolioSnapshotJpaRepository snapshotRepo;
    private final StrategyRepository strategyRepo;
    private final TradingStrategyJpaRepository strategyJpaRepo;

    @Override
    public void run(String... args) throws Exception {
        if (snapshotRepo.count() == 0) {
            log.info("Seeding dummy portfolio data for UI demonstration...");
            PortfolioSnapshotEntity snapshot = PortfolioSnapshotEntity.builder()
                    .portfolioId(UUID.randomUUID())
                    .accountId("DUP854695")
                    .netLiquidationValue(new BigDecimal("125430.50"))
                    .totalCashValue(new BigDecimal("25000.00"))
                    .availableFunds(new BigDecimal("25000.00"))
                    .buyingPower(new BigDecimal("100000.00"))
                    .maintenanceMargin(new BigDecimal("15000.00"))
                    .initialMargin(new BigDecimal("20000.00"))
                    .unrealizedPnl(new BigDecimal("430.50"))
                    .realizedPnl(new BigDecimal("1200.00"))
                    .positionCount(2)
                    .currency("USD")
                    .capturedAt(Instant.now())
                    .build();
            snapshotRepo.save(snapshot);
        }

        if (strategyJpaRepo.count() == 0) {
            log.info("Seeding dummy strategy for UI demonstration...");
            TradingStrategy strategy = TradingStrategy.builder()
                    .id(UUID.randomUUID())
                    .versionId(UUID.randomUUID())
                    .name("SPY Mean Reversion")
                    .description("Buys SPY when RSI < 30 and sells when RSI > 70")
                    .priority(1)
                    .enabled(true)
                    .cooldownMinutes(15)
                    .riskProfile("Moderate")
                    .executionMode("PAPER")
                    .build();
            strategyRepo.save(strategy);
        }
    }
}
