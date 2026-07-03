package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.risk.LimitType;
import com.ibtrader.domain.model.risk.RiskLimit;
import com.ibtrader.domain.port.outbound.RiskLimitRepository;
import com.ibtrader.infrastructure.persistence.entity.RiskLimitEntity;
import com.ibtrader.infrastructure.persistence.mapper.RiskLimitMapper;
import com.ibtrader.infrastructure.persistence.repository.RiskLimitJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Outbound adapter for RiskLimitRepository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskLimitRepositoryAdapter implements RiskLimitRepository {

    private final RiskLimitJpaRepository jpaRepository;
    private final RiskLimitMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<RiskLimit> findByLimitType(LimitType type) {
        return jpaRepository.findByLimitType(type.name())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskLimit> findAllEnabled() {
        return jpaRepository.findByEnabledTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskLimit> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RiskLimit save(RiskLimit limit) {
        RiskLimitEntity entity = mapper.toEntity(limit);
        RiskLimitEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
