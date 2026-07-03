package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.strategy.AllocationTarget;
import com.ibtrader.domain.port.outbound.AllocationTargetRepository;
import com.ibtrader.infrastructure.persistence.entity.AllocationTargetEntity;
import com.ibtrader.infrastructure.persistence.mapper.AllocationTargetMapper;
import com.ibtrader.infrastructure.persistence.repository.AllocationTargetJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter implementation for {@link AllocationTargetRepository}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AllocationTargetRepositoryAdapter implements AllocationTargetRepository {

    private final AllocationTargetJpaRepository jpaRepository;
    private final AllocationTargetMapper mapper;

    @Override
    public List<AllocationTarget> findByStrategyId(UUID strategyId) {
        log.debug("Finding allocation targets for strategy {}", strategyId);
        return jpaRepository.findByStrategyId(strategyId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public AllocationTarget save(AllocationTarget target) {
        log.debug("Saving allocation target {} for strategy {}", target.getId(), target.getStrategyId());
        AllocationTargetEntity entity = mapper.toEntity(target);
        AllocationTargetEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteByStrategyId(UUID strategyId) {
        log.debug("Deleting all allocation targets for strategy {}", strategyId);
        jpaRepository.deleteByStrategyId(strategyId);
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Deleting allocation target {}", id);
        jpaRepository.deleteById(id);
    }
}
