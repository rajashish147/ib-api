package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "portfolio_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class PortfolioSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "account_id", nullable = false, length = 20)
    private String accountId;

    @Column(name = "net_liquidation_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal netLiquidationValue;

    @Column(name = "total_cash_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalCashValue;

    @Column(name = "available_funds", nullable = false, precision = 18, scale = 4)
    private BigDecimal availableFunds;

    @Column(name = "buying_power", nullable = false, precision = 18, scale = 4)
    private BigDecimal buyingPower;

    @Column(name = "maintenance_margin", nullable = false, precision = 18, scale = 4)
    private BigDecimal maintenanceMargin;

    @Column(name = "initial_margin", nullable = false, precision = 18, scale = 4)
    private BigDecimal initialMargin;

    @Column(name = "unrealized_pnl", nullable = false, precision = 18, scale = 4)
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 4)
    private BigDecimal realizedPnl;

    @Column(name = "position_count", nullable = false)
    private Integer positionCount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
