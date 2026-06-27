package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "positions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"asset"})
public class PositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", insertable = false, updatable = false)
    private AssetEntity asset;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "average_cost", nullable = false, precision = 18, scale = 4)
    private BigDecimal averageCost;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "market_price", nullable = false, precision = 18, scale = 4)
    private BigDecimal marketPrice;

    @Column(name = "market_value", nullable = false, precision = 18, scale = 4)
    private BigDecimal marketValue;

    @Column(name = "unrealized_pnl", nullable = false, precision = 18, scale = 4)
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 4)
    private BigDecimal realizedPnl;

    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
