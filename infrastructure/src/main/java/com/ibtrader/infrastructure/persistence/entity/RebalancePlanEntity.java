package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rebalance_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "items")
public class RebalancePlanEntity {

    @Id
    private UUID id;

    private UUID strategyId;

    private String triggerType;

    @Column(name = "strategy_mode")
    private String mode;

    @Column(name = "portfolio_nlv_at_trigger")
    private BigDecimal portfolioNlvAtTrigger;
    
    @Column(name = "currency")
    private String portfolioNlvCurrency;

    @Column(name = "available_budget")
    private BigDecimal availableBudget;
    
    @Column(name = "currency", insertable = false, updatable = false)
    private String availableBudgetCurrency;

    private String status;

    private String notes;

    private Instant createdAt;

    private Instant executedAt;

    private Instant completedAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RebalancePlanItemEntity> items = new ArrayList<>();
}
