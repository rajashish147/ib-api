package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rebalance_plan_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = "plan")
public class RebalancePlanItemEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private RebalancePlanEntity plan;

    private UUID assetId;

    private String symbol;

    private BigDecimal currentWeight;

    private BigDecimal targetWeight;

    private BigDecimal drift;

    private BigDecimal currentQuantity;

    private BigDecimal targetQuantity;

    private BigDecimal quantityDelta;

    private String side;

    @Column(name = "estimated_price")
    private BigDecimal estimatedPriceAmount;
    
    @Column(name = "currency")
    private String estimatedPriceCurrency;

    @Column(name = "estimated_value")
    private BigDecimal estimatedValueAmount;
    
    @Column(name = "currency", insertable = false, updatable = false)
    private String estimatedValueCurrency;

    private UUID orderId;

    private boolean orderPlaced;
}
