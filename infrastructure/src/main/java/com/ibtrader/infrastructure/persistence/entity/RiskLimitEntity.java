package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import java.util.UUID;

@Entity
@Table(name = "risk_limits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class RiskLimitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "limit_type")
    private String limitType;

    @Column(name = "value", precision = 19, scale = 4)
    private BigDecimal value;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "description")
    private String description;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Version
    @Column(name = "version")
    private Long version;
}
