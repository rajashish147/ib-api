package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.infrastructure.persistence.entity.PortfolioSnapshotEntity;
import com.ibtrader.infrastructure.persistence.entity.PositionEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PortfolioMapper {

    private final PositionMapper positionMapper;

    public PortfolioMapper(PositionMapper positionMapper) {
        this.positionMapper = positionMapper;
    }

    public Portfolio toDomain(PortfolioSnapshotEntity snapshot, List<PositionEntity> positionEntities) {
        List<Position> positions = positionEntities.stream()
                .map(positionMapper::toDomain)
                .collect(Collectors.toList());

        return Portfolio.rehydrate(
                snapshot.getPortfolioId(),
                snapshot.getAccountId(),
                Money.of(snapshot.getNetLiquidationValue(), snapshot.getCurrency()),
                Money.of(snapshot.getTotalCashValue(), snapshot.getCurrency()),
                Money.of(snapshot.getAvailableFunds(), snapshot.getCurrency()),
                Money.of(snapshot.getBuyingPower(), snapshot.getCurrency()),
                Money.of(snapshot.getMaintenanceMargin(), snapshot.getCurrency()),
                Money.of(snapshot.getInitialMargin(), snapshot.getCurrency()),
                Money.of(snapshot.getUnrealizedPnl(), snapshot.getCurrency()),
                Money.of(snapshot.getRealizedPnl(), snapshot.getCurrency()),
                positions,
                snapshot.getCapturedAt(),
                0L // We aren't tracking version on snapshot directly, just assume 0 or query max version
        );
    }
}
