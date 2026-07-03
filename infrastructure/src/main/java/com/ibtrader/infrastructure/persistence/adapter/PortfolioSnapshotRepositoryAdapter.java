package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import com.ibtrader.domain.port.outbound.PortfolioSnapshotRepository;
import com.ibtrader.infrastructure.persistence.entity.PortfolioSnapshotEntity;
import com.ibtrader.infrastructure.persistence.repository.PortfolioSnapshotJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@Profile("!demo")
@RequiredArgsConstructor
public class PortfolioSnapshotRepositoryAdapter implements PortfolioSnapshotRepository {

    private final PortfolioSnapshotJpaRepository jpaRepository;

    @Override
    public PortfolioSnapshot save(PortfolioSnapshot snapshot) {
        PortfolioSnapshotEntity entity = PortfolioSnapshotEntity.builder()
                .id(snapshot.getId())
                .portfolioId(snapshot.getPortfolioId())
                .accountId(snapshot.getAccountId())
                .netLiquidationValue(snapshot.getNetLiquidationValue().getAmount())
                .totalCashValue(snapshot.getTotalCashValue().getAmount())
                .availableFunds(snapshot.getAvailableFunds().getAmount())
                .buyingPower(snapshot.getBuyingPower().getAmount())
                .maintenanceMargin(snapshot.getMaintenanceMargin().getAmount())
                .initialMargin(BigDecimal.ZERO)
                .unrealizedPnl(snapshot.getUnrealizedPnL().getAmount())
                .realizedPnl(snapshot.getRealizedPnL().getAmount())
                .positionCount(snapshot.getPositionCount())
                .currency(snapshot.getNetLiquidationValue().getCurrency())
                .capturedAt(snapshot.getCapturedAt())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<PortfolioSnapshot> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<PortfolioSnapshot> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PortfolioSnapshot> findByAccountId(String accountId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        return jpaRepository.findByAccountIdOrderByCapturedAtDesc(accountId, PageRequest.of(0, boundedLimit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private PortfolioSnapshot toDomain(PortfolioSnapshotEntity entity) {
        String currency = entity.getCurrency();
        return PortfolioSnapshot.builder()
                .id(entity.getId())
                .portfolioId(entity.getPortfolioId())
                .accountId(entity.getAccountId())
                .netLiquidationValue(Money.of(entity.getNetLiquidationValue(), currency))
                .totalCashValue(Money.of(entity.getTotalCashValue(), currency))
                .availableFunds(Money.of(entity.getAvailableFunds(), currency))
                .buyingPower(Money.of(entity.getBuyingPower(), currency))
                .maintenanceMargin(Money.of(entity.getMaintenanceMargin(), currency))
                .unrealizedPnL(Money.of(entity.getUnrealizedPnl(), currency))
                .realizedPnL(Money.of(entity.getRealizedPnl(), currency))
                .positionCount(entity.getPositionCount())
                .capturedAt(entity.getCapturedAt())
                .build();
    }
}
