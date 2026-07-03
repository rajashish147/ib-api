package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.portfolio.Position;
import com.ibtrader.domain.port.outbound.PositionRepository;
import com.ibtrader.infrastructure.persistence.entity.PositionEntity;
import com.ibtrader.infrastructure.persistence.mapper.PositionMapper;
import com.ibtrader.infrastructure.persistence.repository.PositionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Outbound adapter for PositionRepository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PositionRepositoryAdapter implements PositionRepository {

    private final PositionJpaRepository jpaRepository;
    private final PositionMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<Position> findByPortfolioId(UUID portfolioId) {
        return jpaRepository.findByPortfolioId(portfolioId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Position> findByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId) {
        return jpaRepository.findByPortfolioIdAndAssetId(portfolioId, assetId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Position save(Position position) {
        PositionEntity entity = mapper.toEntity(position);
        PositionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId) {
        jpaRepository.deleteByPortfolioIdAndAssetId(portfolioId, assetId);
    }

    @Override
    @Transactional
    public void deleteAllByPortfolioId(UUID portfolioId) {
        jpaRepository.deleteAllByPortfolioId(portfolioId);
    }
}
