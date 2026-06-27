package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class EvaluationHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID strategyId;
    private UUID strategyVersionId;
    private Instant evaluationTime;
    private UUID portfolioSnapshotId;
    
    @Column(columnDefinition = "jsonb")
    private String matchedRules;
    
    @Column(columnDefinition = "jsonb")
    private String unmatchedRules;
    
    @Column(columnDefinition = "jsonb")
    private String generatedSignals;
    
    @Column(columnDefinition = "jsonb")
    private String rejectedSignals;
    
    @Column(columnDefinition = "jsonb")
    private String rejectionReasons;
    
    @Column(columnDefinition = "jsonb")
    private String plannedOrders;
    
    private String executionOutcome;
}
