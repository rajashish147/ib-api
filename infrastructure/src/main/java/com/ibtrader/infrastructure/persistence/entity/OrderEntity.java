package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ib_order_id", unique = true)
    private Integer ibOrderId;

    @Column(name = "account_id", nullable = false, length = 20)
    private String accountId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @Column(nullable = false, length = 10)
    private String side;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "filled_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal filledQuantity;

    @Column(name = "remaining_quantity", nullable = false, precision = 18, scale = 6)
    private BigDecimal remainingQuantity;

    @Column(name = "limit_price", precision = 18, scale = 4)
    private BigDecimal limitPrice;

    @Column(name = "stop_price", precision = 18, scale = 4)
    private BigDecimal stopPrice;

    @Column(name = "avg_fill_price", precision = 18, scale = 4)
    private BigDecimal avgFillPrice;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "strategy_ref", length = 100)
    private String strategyRef;

    @Column(name = "rebalance_plan_id")
    private UUID rebalancePlanId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

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
        if (this.lastUpdatedAt == null) {
            this.lastUpdatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
