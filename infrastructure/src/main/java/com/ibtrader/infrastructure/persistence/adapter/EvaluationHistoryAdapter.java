package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.strategy.EvaluationHistory;
import com.ibtrader.domain.port.outbound.EvaluationHistoryRepository;
import com.ibtrader.infrastructure.persistence.mapper.EvaluationHistoryMapper;
import com.ibtrader.infrastructure.persistence.repository.EvaluationHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for EvaluationHistory.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationHistoryAdapter implements EvaluationHistoryRepository<EvaluationHistory> {

    private final EvaluationHistoryJpaRepository jpaRepository;
    private final EvaluationHistoryMapper mapper;

    @Override
    public EvaluationHistory save(EvaluationHistory history) {
        log.debug("Saving EvaluationHistory with id: {}", history.getId());
        var entity = mapper.toEntity(history);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<EvaluationHistory> findById(String id) {
        log.debug("Finding EvaluationHistory by id: {}", id);
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<EvaluationHistory> findAll() {
        log.debug("Finding all EvaluationHistories");
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        log.debug("Deleting EvaluationHistory by id: {}", id);
        jpaRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public Optional<EvaluationHistory> findLastByStrategyId(UUID strategyId) {
        log.debug("Finding last EvaluationHistory for strategy {}", strategyId);
        return jpaRepository.findFirstByStrategyIdOrderByEvaluationTimeDesc(strategyId)
                .map(mapper::toDomain);
    }
}
