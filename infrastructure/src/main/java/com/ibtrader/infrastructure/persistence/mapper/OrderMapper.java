package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.order.OrderStatus;
import com.ibtrader.domain.model.order.OrderType;
import com.ibtrader.infrastructure.persistence.entity.OrderEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Mapper for converting between Order domain models and entities.
 */
@Component
public class OrderMapper {

    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        Money limitPrice = entity.getLimitPrice() != null ? Money.of(entity.getLimitPrice(), entity.getCurrency()) : null;
        Money stopPrice = entity.getStopPrice() != null ? Money.of(entity.getStopPrice(), entity.getCurrency()) : null;
        Money avgFillPrice = entity.getAvgFillPrice() != null ? Money.of(entity.getAvgFillPrice(), entity.getCurrency()) : null;

        return Order.rehydrate(
                entity.getId(),
                entity.getIbOrderId(),
                entity.getAccountId(),
                entity.getAssetId(),
                entity.getSymbol(),
                entity.getOrderType() != null ? OrderType.valueOf(entity.getOrderType()) : null,
                entity.getSide() != null ? OrderSide.valueOf(entity.getSide()) : null,
                entity.getQuantity(),
                entity.getFilledQuantity() != null ? entity.getFilledQuantity() : BigDecimal.ZERO,
                limitPrice,
                stopPrice,
                avgFillPrice,
                entity.getStatus() != null ? OrderStatus.valueOf(entity.getStatus()) : null,
                entity.getStrategyRef(),
                entity.getRebalancePlanId(),
                new ArrayList<>(),
                entity.getSubmittedAt(),
                entity.getLastUpdatedAt(),
                entity.getRejectionReason(),
                entity.getVersion() != null ? entity.getVersion() : 0L
        );
    }

    public OrderEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }

        String currency = "USD";
        if (domain.getLimitPrice() != null) currency = domain.getLimitPrice().getCurrency();
        else if (domain.getStopPrice() != null) currency = domain.getStopPrice().getCurrency();
        else if (domain.getAverageFillPrice() != null) currency = domain.getAverageFillPrice().getCurrency();

        return OrderEntity.builder()
                .id(domain.getId())
                .ibOrderId(domain.getIbOrderId())
                .accountId(domain.getAccountId())
                .assetId(domain.getAssetId())
                .symbol(domain.getSymbol())
                .orderType(domain.getOrderType() != null ? domain.getOrderType().name() : null)
                .side(domain.getSide() != null ? domain.getSide().name() : null)
                .quantity(domain.getQuantity())
                .filledQuantity(domain.getFilledQuantity() != null ? domain.getFilledQuantity() : BigDecimal.ZERO)
                .remainingQuantity(domain.getRemainingQuantity())
                .limitPrice(domain.getLimitPrice() != null ? domain.getLimitPrice().getAmount() : null)
                .stopPrice(domain.getStopPrice() != null ? domain.getStopPrice().getAmount() : null)
                .avgFillPrice(domain.getAverageFillPrice() != null ? domain.getAverageFillPrice().getAmount() : null)
                .currency(currency)
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .strategyRef(domain.getStrategyRef())
                .rebalancePlanId(domain.getRebalancePlanId())
                .rejectionReason(domain.getRejectionReason())
                .submittedAt(domain.getSubmittedAt())
                .lastUpdatedAt(domain.getLastUpdatedAt())
                .version(domain.getVersion())
                .build();
    }
}
