package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.portfolio.PortfolioGoal;
import com.ibtrader.domain.port.outbound.PortfolioGoalRepository;
import com.ibtrader.infrastructure.persistence.mapper.PortfolioGoalMapper;
import com.ibtrader.infrastructure.persistence.repository.PortfolioGoalJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Persistence adapter for PortfolioGoal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioGoalAdapter implements PortfolioGoalRepository<PortfolioGoal> {

    private final PortfolioGoalJpaRepository jpaRepository;
    private final PortfolioGoalMapper mapper;

    @Override
    public PortfolioGoal save(PortfolioGoal goal) {
        log.debug("Saving PortfolioGoal with id: {}", goal.getId());
        var entity = mapper.toEntity(goal);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<PortfolioGoal> findById(String id) {
        log.debug("Finding PortfolioGoal by id: {}", id);
        return jpaRepository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<PortfolioGoal> findAll() {
        log.debug("Finding all PortfolioGoals");
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        log.debug("Deleting PortfolioGoal by id: {}", id);
        jpaRepository.deleteById(UUID.fromString(id));
    }
}
