package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "executions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class ExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "ib_order_id", nullable = false)
    private Integer ibOrderId;

    @Column(name = "exec_id", nullable = false, unique = true, length = 100)
    private String execId;

    @Column(name = "account_id", nullable = false, length = 20)
    private String accountId;

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String side;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal commission;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 4)
    private BigDecimal realizedPnl;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(length = 20)
    private String exchange;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
