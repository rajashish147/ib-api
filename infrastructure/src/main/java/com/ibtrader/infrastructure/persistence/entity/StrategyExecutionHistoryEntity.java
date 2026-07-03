package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "strategy_execution_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyExecutionHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "strategy_id", nullable = false)
    private UUID strategyId;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "successful", nullable = false)
    private boolean successful;

    @Column(name = "reason", length = 1000)
    private String reason;
}
