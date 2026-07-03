package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.strategy.RebalancePlan;
import com.ibtrader.domain.port.outbound.RebalancePlanRepository;
import com.ibtrader.infrastructure.persistence.entity.RebalancePlanEntity;
import com.ibtrader.infrastructure.persistence.mapper.RebalancePlanMapper;
import com.ibtrader.infrastructure.persistence.repository.RebalancePlanJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation for {@link RebalancePlanRepository}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RebalancePlanRepositoryAdapter implements RebalancePlanRepository {

    private final RebalancePlanJpaRepository jpaRepository;
    private final RebalancePlanMapper mapper;

    @Override
    public Optional<RebalancePlan> findById(UUID id) {
        log.debug("Finding rebalance plan by id {}", id);
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RebalancePlan> findByStrategyId(UUID strategyId) {
        log.debug("Finding rebalance plans for strategy {}", strategyId);
        return jpaRepository.findByStrategyIdOrderByCreatedAtDesc(strategyId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RebalancePlan> findLatestByStrategyId(UUID strategyId) {
        log.debug("Finding latest rebalance plan for strategy {}", strategyId);
        return jpaRepository.findFirstByStrategyIdOrderByCreatedAtDesc(strategyId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public RebalancePlan save(RebalancePlan plan) {
        log.debug("Saving rebalance plan {} for strategy {}", plan.getId(), plan.getStrategyId());
        RebalancePlanEntity entity = mapper.toEntity(plan);
        RebalancePlanEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
