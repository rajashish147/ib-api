package com.ibtrader.infrastructure.persistence.entity;

import com.ibtrader.domain.model.order.OrderSide;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trade_signals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class TradeSignalEntity {
    @Id
    private UUID id;

    private UUID strategyId;
    private String symbol;

    @Enumerated(EnumType.STRING)
    private OrderSide action;

    private String quantityType;
    private BigDecimal quantityValue;
    private String reason;
    private double confidence;
    private Instant generatedAt;
}
