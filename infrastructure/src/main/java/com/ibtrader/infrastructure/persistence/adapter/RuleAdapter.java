package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.strategy.RuleAction;
import com.ibtrader.domain.port.outbound.RuleRepository;
import com.ibtrader.infrastructure.persistence.mapper.RuleActionMapper;
import com.ibtrader.infrastructure.persistence.repository.RuleActionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for Rule (RuleAction).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleAdapter implements RuleRepository {

    private final RuleActionJpaRepository jpaRepository;
    private final RuleActionMapper mapper;

    @Override
    public RuleAction save(RuleAction rule) {
        log.debug("Saving RuleAction with id: {}", rule.getId());
        var entity = mapper.toEntity(rule);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<RuleAction> findById(UUID id) {
        log.debug("Finding RuleAction by id: {}", id);
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<RuleAction> findByStrategyId(UUID strategyId) {
        log.debug("Finding RuleActions by strategyId: {}", strategyId);
        return jpaRepository.findByStrategyVersionId(strategyId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<RuleAction> findAll() {
        log.debug("Finding all RuleActions");
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Deleting RuleAction by id: {}", id);
        jpaRepository.deleteById(id);
    }
}
