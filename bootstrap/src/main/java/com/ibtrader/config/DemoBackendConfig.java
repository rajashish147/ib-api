package com.ibtrader.config;

import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.asset.AssetClass;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderStatus;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.domain.model.risk.LimitType;
import com.ibtrader.domain.model.risk.RiskLimit;
import com.ibtrader.domain.model.strategy.ExpressionNode;
import com.ibtrader.domain.model.strategy.RebalancePlan;
import com.ibtrader.domain.model.strategy.StrategyState;
import com.ibtrader.domain.model.strategy.StrategyExecutionHistory;
import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.outbound.AccountDataPort;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.ExpressionTreeRepository;
import com.ibtrader.domain.port.outbound.IbCommandOutboxPort;
import com.ibtrader.domain.port.outbound.OrderRepository;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.PortfolioSnapshotRepository;
import com.ibtrader.domain.port.outbound.PositionRepository;
import com.ibtrader.domain.port.outbound.RebalancePlanRepository;
import com.ibtrader.domain.port.outbound.RiskLimitRepository;
import com.ibtrader.domain.port.outbound.StrategyExecutionHistoryPort;
import com.ibtrader.domain.port.outbound.StrategyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@Profile("demo")
public class DemoBackendConfig {

    private static final String DEMO_ACCOUNT_ID = "DUP854695";

    @Bean
    public AssetRepository demoAssetRepository() {
        return new InMemoryAssetRepository();
    }

    @Bean
    public PortfolioRepository demoPortfolioRepository() {
        return new InMemoryPortfolioRepository();
    }

    @Bean
    public PortfolioSnapshotRepository demoPortfolioSnapshotRepository() {
        return new InMemoryPortfolioSnapshotRepository();
    }

    @Bean
    public StrategyRepository demoStrategyRepository() {
        return new InMemoryStrategyRepository();
    }

    @Bean
    public RiskLimitRepository demoRiskLimitRepository() {
        return new InMemoryRiskLimitRepository();
    }

    @Bean
    public PositionRepository demoPositionRepository() {
        return new InMemoryPositionRepository();
    }

    @Bean
    public OrderRepository demoOrderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    public RebalancePlanRepository demoRebalancePlanRepository() {
        return new InMemoryRebalancePlanRepository();
    }

    @Bean
    public StrategyExecutionHistoryPort demoStrategyExecutionHistoryPort() {
        return new InMemoryStrategyExecutionHistoryPort();
    }

    @Bean
    public AccountDataPort demoAccountDataPort() {
        return new NoOpAccountDataPort();
    }

    @Bean
    public ExpressionTreeRepository demoExpressionTreeRepository() {
        return new EmptyExpressionTreeRepository();
    }

    @Bean
    public IbCommandOutboxPort demoIbCommandOutboxPort() {
        return plan -> log.info(
                "DEMO outbox queued: {} {} {} for strategy {}",
                plan.getSide(),
                plan.getTargetQuantity(),
                plan.getSymbol(),
                plan.getStrategyId());
    }

    private static final class InMemoryAssetRepository implements AssetRepository {

        private final Map<UUID, Asset> assets = new ConcurrentHashMap<>();

        private InMemoryAssetRepository() {
            List.of("AAPL", "MSFT", "SNDK", "META", "NVDA").forEach(symbol -> save(
                    Asset.create(symbol, "SMART", "USD", AssetClass.STOCK)));
        }

        @Override
        public Optional<Asset> findById(UUID id) {
            return Optional.ofNullable(assets.get(id));
        }

        @Override
        public Optional<Asset> findBySymbol(String symbol) {
            return assets.values().stream()
                    .filter(asset -> asset.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst();
        }

        @Override
        public Optional<Asset> findByIbConId(int conId) {
            return assets.values().stream()
                    .filter(asset -> asset.getIbConId() != null && asset.getIbConId() == conId)
                    .findFirst();
        }

        @Override
        public List<Asset> findAllEnabled() {
            return assets.values().stream()
                    .filter(Asset::isEnabled)
                    .sorted(Comparator.comparing(Asset::getSymbol))
                    .toList();
        }

        @Override
        public List<Asset> findAll() {
            return assets.values().stream()
                    .sorted(Comparator.comparing(Asset::getSymbol))
                    .toList();
        }

        @Override
        public Asset save(Asset asset) {
            assets.put(asset.getId(), asset);
            return asset;
        }

        @Override
        public void deleteById(UUID id) {
            assets.remove(id);
        }
    }

    private static final class InMemoryPortfolioRepository implements PortfolioRepository {

        private final Map<String, Portfolio> portfolios = new ConcurrentHashMap<>();

        private InMemoryPortfolioRepository() {
            Portfolio portfolio = Portfolio.create(DEMO_ACCOUNT_ID);
            portfolio.updateAccountValues(
                    Money.usd(new BigDecimal("10000.00")),
                    Money.usd(new BigDecimal("10000.00")),
                    Money.usd(new BigDecimal("10000.00")),
                    Money.usd(new BigDecimal("40000.00")),
                    Money.usd(BigDecimal.ZERO),
                    Money.usd(BigDecimal.ZERO),
                    Money.usd(BigDecimal.ZERO),
                    Money.usd(BigDecimal.ZERO));
            save(portfolio);
        }

        @Override
        public Optional<Portfolio> findByAccountId(String accountId) {
            return Optional.ofNullable(portfolios.get(accountId));
        }

        @Override
        public Portfolio save(Portfolio portfolio) {
            portfolios.put(portfolio.getAccountId(), portfolio);
            return portfolio;
        }

        @Override
        public void delete(UUID id) {
            portfolios.values().removeIf(portfolio -> portfolio.getId().equals(id));
        }
    }

    private static final class InMemoryPortfolioSnapshotRepository implements PortfolioSnapshotRepository {

        private final Map<UUID, PortfolioSnapshot> snapshots = new ConcurrentHashMap<>();

        @Override
        public PortfolioSnapshot save(PortfolioSnapshot snapshot) {
            snapshots.put(snapshot.getId(), snapshot);
            return snapshot;
        }

        @Override
        public Optional<PortfolioSnapshot> findById(UUID id) {
            return Optional.ofNullable(snapshots.get(id));
        }

        @Override
        public List<PortfolioSnapshot> findAll() {
            return snapshots.values().stream()
                    .sorted(Comparator.comparing(PortfolioSnapshot::getCapturedAt).reversed())
                    .toList();
        }

        @Override
        public List<PortfolioSnapshot> findByAccountId(String accountId, int limit) {
            return snapshots.values().stream()
                    .filter(snapshot -> snapshot.getAccountId().equals(accountId))
                    .sorted(Comparator.comparing(PortfolioSnapshot::getCapturedAt).reversed())
                    .limit(Math.max(1, Math.min(limit, 500)))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            snapshots.remove(id);
        }
    }

    private static final class InMemoryPositionRepository implements PositionRepository {

        private final Map<UUID, Position> positions = new ConcurrentHashMap<>();

        @Override
        public List<Position> findByPortfolioId(UUID portfolioId) {
            return positions.values().stream()
                    .filter(position -> position.getPortfolioId().equals(portfolioId))
                    .sorted(Comparator.comparing(Position::getSymbol))
                    .toList();
        }

        @Override
        public Optional<Position> findByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId) {
            return positions.values().stream()
                    .filter(position -> position.getPortfolioId().equals(portfolioId))
                    .filter(position -> position.getAssetId().equals(assetId))
                    .findFirst();
        }

        @Override
        public Position save(Position position) {
            positions.put(position.getId(), position);
            return position;
        }

        @Override
        public void deleteByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId) {
            positions.values().removeIf(position -> position.getPortfolioId().equals(portfolioId)
                    && position.getAssetId().equals(assetId));
        }

        @Override
        public void deleteAllByPortfolioId(UUID portfolioId) {
            positions.values().removeIf(position -> position.getPortfolioId().equals(portfolioId));
        }
    }

    private static final class InMemoryStrategyRepository implements StrategyRepository {

        private final Map<UUID, TradingStrategy> strategies = new ConcurrentHashMap<>();

        @Override
        public TradingStrategy save(TradingStrategy strategy) {
            TradingStrategy saved = TradingStrategy.builder()
                    .id(strategy.getId() == null ? UUID.randomUUID() : strategy.getId())
                    .versionId(strategy.getVersionId() == null ? UUID.randomUUID() : strategy.getVersionId())
                    .name(strategy.getName())
                    .description(strategy.getDescription())
                    .priority(strategy.getPriority())
                    .enabled(strategy.isEnabled())
                    .cooldownMinutes(strategy.getCooldownMinutes())
                    .riskProfile(strategy.getRiskProfile())
                    .executionMode(strategy.getExecutionMode())
                    .buyThreshold(strategy.getBuyThreshold())
                    .sellThreshold(strategy.getSellThreshold())
                    .state(strategy.getState() == null ? StrategyState.IDLE : strategy.getState())
                    .targets(strategy.getTargets() == null ? List.of() : new ArrayList<>(strategy.getTargets()))
                    .build();
            strategies.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<TradingStrategy> findById(UUID id) {
            return Optional.ofNullable(strategies.get(id));
        }

        @Override
        public List<TradingStrategy> findActiveStrategies() {
            return strategies.values().stream()
                    .filter(TradingStrategy::isEnabled)
                    .sorted(Comparator.comparingInt(TradingStrategy::getPriority))
                    .toList();
        }

        @Override
        public List<TradingStrategy> findAll() {
            return strategies.values().stream()
                    .sorted(Comparator.comparingInt(TradingStrategy::getPriority))
                    .toList();
        }

        @Override
        public void deleteById(UUID id) {
            strategies.remove(id);
        }
    }

    private static final class InMemoryRiskLimitRepository implements RiskLimitRepository {

        private final Map<LimitType, RiskLimit> limits = new ConcurrentHashMap<>();

        @Override
        public Optional<RiskLimit> findByLimitType(LimitType type) {
            return Optional.ofNullable(limits.get(type));
        }

        @Override
        public List<RiskLimit> findAllEnabled() {
            return limits.values().stream()
                    .filter(RiskLimit::isEnabled)
                    .toList();
        }

        @Override
        public List<RiskLimit> findAll() {
            return List.copyOf(limits.values());
        }

        @Override
        public RiskLimit save(RiskLimit limit) {
            limits.put(limit.getLimitType(), limit);
            return limit;
        }
    }

    private static final class InMemoryOrderRepository implements OrderRepository {

        private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(orders.get(id));
        }

        @Override
        public Optional<Order> findByIbOrderId(int ibOrderId) {
            return orders.values().stream()
                    .filter(order -> order.getIbOrderId() != null && order.getIbOrderId() == ibOrderId)
                    .findFirst();
        }

        @Override
        public List<Order> findByStatus(OrderStatus status) {
            return orders.values().stream()
                    .filter(order -> order.getStatus() == status)
                    .toList();
        }

        @Override
        public List<Order> findByRebalancePlanId(UUID planId) {
            return orders.values().stream()
                    .filter(order -> planId.equals(order.getRebalancePlanId()))
                    .toList();
        }

        @Override
        public List<Order> findOpenOrders() {
            return orders.values().stream()
                    .filter(order -> order.getStatus() == OrderStatus.SUBMITTED
                            || order.getStatus() == OrderStatus.PARTIALLY_FILLED)
                    .toList();
        }

        @Override
        public Order save(Order order) {
            orders.put(order.getId(), order);
            return order;
        }

        @Override
        public List<Order> findAll(int page, int size) {
            return orders.values().stream()
                    .sorted(Comparator.comparing(Order::getSubmittedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .skip((long) Math.max(page, 0) * Math.max(size, 1))
                    .limit(Math.max(size, 1))
                    .toList();
        }

        @Override
        public List<Order> findByAssetId(UUID assetId) {
            return orders.values().stream()
                    .filter(order -> assetId.equals(order.getAssetId()))
                    .toList();
        }
    }

    private static final class InMemoryRebalancePlanRepository implements RebalancePlanRepository {

        private final Map<UUID, RebalancePlan> plans = new ConcurrentHashMap<>();

        @Override
        public Optional<RebalancePlan> findById(UUID id) {
            return Optional.ofNullable(plans.get(id));
        }

        @Override
        public List<RebalancePlan> findByStrategyId(UUID strategyId) {
            return plans.values().stream()
                    .filter(plan -> plan.getStrategyId().equals(strategyId))
                    .sorted(Comparator.comparing(RebalancePlan::getCreatedAt).reversed())
                    .toList();
        }

        @Override
        public Optional<RebalancePlan> findLatestByStrategyId(UUID strategyId) {
            return findByStrategyId(strategyId).stream().findFirst();
        }

        @Override
        public RebalancePlan save(RebalancePlan plan) {
            plans.put(plan.getId(), plan);
            return plan;
        }
    }

    private static final class InMemoryStrategyExecutionHistoryPort implements StrategyExecutionHistoryPort {

        private final Map<UUID, List<StrategyExecutionHistory>> histories = new ConcurrentHashMap<>();

        @Override
        public void save(StrategyExecutionHistory history) {
            histories.computeIfAbsent(history.getStrategyId(), ignored -> new ArrayList<>()).add(history);
        }

        @Override
        public Optional<StrategyExecutionHistory> findLastSuccessfulExecution(UUID strategyId) {
            return histories.getOrDefault(strategyId, List.of()).stream()
                    .filter(StrategyExecutionHistory::isSuccessful)
                    .max(Comparator.comparing(StrategyExecutionHistory::getExecutedAt));
        }
    }

    private static final class NoOpAccountDataPort implements AccountDataPort {

        @Override
        public void requestAccountUpdates(String accountId) {
        }

        @Override
        public void requestPositions() {
        }

        @Override
        public void requestExecutions() {
        }

        @Override
        public boolean isAccountSyncActive() {
            return true;
        }
    }

    private static final class EmptyExpressionTreeRepository implements ExpressionTreeRepository {

        @Override
        public ExpressionNode save(ExpressionNode node) {
            return node;
        }

        @Override
        public Optional<ExpressionNode> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<ExpressionNode> findByStrategyId(UUID strategyId) {
            return Optional.empty();
        }

        @Override
        public List<ExpressionNode> findAll() {
            return List.of();
        }

        @Override
        public void deleteById(UUID id) {
        }
    }
}
