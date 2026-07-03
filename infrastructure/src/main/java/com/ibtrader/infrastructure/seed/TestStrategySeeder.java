package com.ibtrader.infrastructure.seed;

import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.asset.AssetClass;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.strategy.BasketTarget;
import com.ibtrader.domain.model.strategy.StrategyState;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.StrategyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class TestStrategySeeder implements CommandLineRunner {

    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;
    private final StrategyRepository strategyRepository;

    @Value("${app.ib.account-id:DUP854695}")
    private String accountId;

    @Override
    public void run(String... args) {
        log.info("Running Test Strategy Seeder...");

        // 1. Seed Portfolio
        if (portfolioRepository.findByAccountId(accountId).isEmpty()) {
            log.info("Creating default portfolio for account: {}", accountId);
            Portfolio portfolio = Portfolio.create(accountId);
            portfolioRepository.save(portfolio);
        }

        // 2. Seed Assets
        List<String> symbols = List.of("SNDK", "META", "NVDA");
        symbols.forEach(symbol -> {
            if (assetRepository.findBySymbol(symbol).isEmpty()) {
                log.info("Creating test asset: {}", symbol);
                Asset asset = Asset.create(symbol, "SMART", "USD", AssetClass.STOCK);
                assetRepository.save(asset);
            }
        });

        // 3. Seed Trading Strategy (FIXED_AMOUNT of 1 unit per asset)
        if (strategyRepository.findAll().isEmpty()) {
            log.info("Creating FIXED_AMOUNT Test Strategy for SNDK, META, NVDA");

            List<BasketTarget> targets = symbols.stream().map(symbol -> {
                Asset asset = assetRepository.findBySymbol(symbol).orElseThrow();
                return BasketTarget.builder()
                        .id(asset.getId())
                        .symbol(symbol)
                        .assetClass(AssetClass.STOCK.name())
                        .quantity(BigDecimal.ONE)
                        .build();
            }).collect(Collectors.toList());

            TradingStrategy strategy = TradingStrategy.builder()
                    .id(UUID.randomUUID())
                    .versionId(UUID.randomUUID())
                    .name("Live Browser Test Strategy")
                    .description("Test strategy buying 1 unit at close margin.")
                    .priority(1)
                    .enabled(true)
                    .cooldownMinutes(1)
                    .riskProfile("AGGRESSIVE")
                    .executionMode("PAPER")
                    // Example absolute margins (close margins)
                    .buyThreshold(BigDecimal.valueOf(150.0))
                    .sellThreshold(BigDecimal.valueOf(1000.0))
                    .state(StrategyState.IDLE)
                    .targets(targets)
                    .build();

            strategyRepository.save(strategy);
        }
    }
}
