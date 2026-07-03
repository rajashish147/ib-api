package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.strategy.EvaluationHistory;
import com.ibtrader.infrastructure.persistence.entity.EvaluationHistoryEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper component for EvaluationHistory domain model and EvaluationHistoryEntity.
 */
@Component
public class EvaluationHistoryMapper {

    public EvaluationHistory toDomain(EvaluationHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return EvaluationHistory.builder()
                .id(entity.getId())
                .strategyId(entity.getStrategyId())
                .strategyVersionId(entity.getStrategyVersionId())
                .evaluationTime(entity.getEvaluationTime())
                .portfolioSnapshotId(entity.getPortfolioSnapshotId())
                .matchedRules(entity.getMatchedRules())
                .unmatchedRules(entity.getUnmatchedRules())
                .generatedSignals(entity.getGeneratedSignals())
                .rejectedSignals(entity.getRejectedSignals())
                .rejectionReasons(entity.getRejectionReasons())
                .plannedOrders(entity.getPlannedOrders())
                .executionOutcome(entity.getExecutionOutcome())
                .build();
    }

    public EvaluationHistoryEntity toEntity(EvaluationHistory domain) {
        if (domain == null) {
            return null;
        }
        return EvaluationHistoryEntity.builder()
                .id(domain.getId())
                .strategyId(domain.getStrategyId())
                .strategyVersionId(domain.getStrategyVersionId())
                .evaluationTime(domain.getEvaluationTime())
                .portfolioSnapshotId(domain.getPortfolioSnapshotId())
                .matchedRules(domain.getMatchedRules())
                .unmatchedRules(domain.getUnmatchedRules())
                .generatedSignals(domain.getGeneratedSignals())
                .rejectedSignals(domain.getRejectedSignals())
                .rejectionReasons(domain.getRejectionReasons())
                .plannedOrders(domain.getPlannedOrders())
                .executionOutcome(domain.getExecutionOutcome())
                .build();
    }
}
