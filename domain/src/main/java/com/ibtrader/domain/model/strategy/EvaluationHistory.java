package com.ibtrader.domain.model.strategy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing an evaluation history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationHistory {
    private UUID id;
    private UUID strategyId;
    private UUID strategyVersionId;
    private Instant evaluationTime;
    private UUID portfolioSnapshotId;
    private String matchedRules;
    private String unmatchedRules;
    private String generatedSignals;
    private String rejectedSignals;
    private String rejectionReasons;
    private String plannedOrders;
    private String executionOutcome;
}
