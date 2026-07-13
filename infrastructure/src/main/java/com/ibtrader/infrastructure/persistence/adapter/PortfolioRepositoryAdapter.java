package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.portfolio.Portfolio;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.infrastructure.persistence.entity.PortfolioSnapshotEntity;
import com.ibtrader.infrastructure.persistence.entity.PositionEntity;
import com.ibtrader.infrastructure.persistence.mapper.PortfolioMapper;
import com.ibtrader.infrastructure.persistence.mapper.PositionMapper;
import com.ibtrader.infrastructure.persistence.repository.PortfolioSnapshotJpaRepository;
import com.ibtrader.infrastructure.persistence.repository.PositionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@Profile("!demo")
@RequiredArgsConstructor
public class PortfolioRepositoryAdapter implements PortfolioRepository {

    private final PositionJpaRepository positionJpaRepository;
    private final PortfolioSnapshotJpaRepository snapshotJpaRepository;
    private final PortfolioMapper portfolioMapper;
    private final PositionMapper positionMapper;

    @Override
    public Optional<Portfolio> findByAccountId(String accountId) {
        Optional<PortfolioSnapshotEntity> snapshotOpt = snapshotJpaRepository.findFirstByAccountIdOrderByCreatedAtDesc(accountId);
        if (snapshotOpt.isEmpty()) {
            return Optional.empty();
        }
        
        PortfolioSnapshotEntity snapshot = snapshotOpt.get();
        List<PositionEntity> positions = positionJpaRepository.findByPortfolioId(snapshot.getPortfolioId());
        
        return Optional.of(portfolioMapper.toDomain(snapshot, positions));
    }

    @Override
    @Transactional
    public Portfolio save(Portfolio portfolio) {
        // Save positions atomically -- without an explicit transaction boundary, each
        // individual save() call below would commit independently, risking partially
        // persisted positions if a later save in the loop fails.
        List<PositionEntity> entities = portfolio.getPositions().stream()
                .map(positionMapper::toEntity)
                .toList();
        positionJpaRepository.saveAll(entities);
        return portfolio;
    }

    @Override
    public void delete(UUID id) {
        positionJpaRepository.deleteAllByPortfolioId(id);
    }
}
