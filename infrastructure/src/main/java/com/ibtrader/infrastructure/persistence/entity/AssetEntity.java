package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 20)
    private String exchange;

    @Column(nullable = false, length = 10)
    private String currency;

    /**
     * Stored as a plain String (not enum) to allow schema flexibility without requiring
     * application-level enum changes when new asset classes are introduced.
     */
    @Column(name = "asset_class", nullable = false, length = 20)
    private String assetClass;

    @Column(name = "ib_con_id")
    private Integer ibConId;

    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal multiplier;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "local_symbol", length = 50)
    private String localSymbol;

    @Column(nullable = false)
    private boolean enabled;

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
