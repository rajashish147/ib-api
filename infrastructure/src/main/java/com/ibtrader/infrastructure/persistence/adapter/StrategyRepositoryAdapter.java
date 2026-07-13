package com.ibtrader.infrastructure.persistence.adapter;


import com.ibtrader.domain.model.strategy.TradingStrategy;
import com.ibtrader.domain.port.outbound.StrategyRepository;
import com.ibtrader.infrastructure.persistence.entity.StrategyBasketTargetEntity;
import com.ibtrader.infrastructure.persistence.entity.TradingStrategyEntity;
import com.ibtrader.infrastructure.persistence.entity.TradingStrategyVersionEntity;
import com.ibtrader.infrastructure.persistence.mapper.StrategyMapper;
import com.ibtrader.infrastructure.persistence.repository.StrategyBasketTargetJpaRepository;
import com.ibtrader.infrastructure.persistence.repository.TradingStrategyJpaRepository;
import com.ibtrader.infrastructure.persistence.repository.TradingStrategyVersionJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Profile("!demo")
@RequiredArgsConstructor
public class StrategyRepositoryAdapter implements StrategyRepository {

    private final TradingStrategyJpaRepository strategyJpaRepository;
    private final TradingStrategyVersionJpaRepository versionJpaRepository;
    private final StrategyBasketTargetJpaRepository targetJpaRepository;
    private final StrategyMapper strategyMapper;

    @Override
    @Transactional
    public TradingStrategy save(TradingStrategy strategy) {
        UUID strategyId = strategy.getId() != null ? strategy.getId() : UUID.randomUUID();
        
        TradingStrategyEntity entity = strategyJpaRepository.findById(strategyId)
                .orElseGet(() -> TradingStrategyEntity.builder()
                        .id(strategyId)
                        .version(0L)
                        .createdAt(java.time.Instant.now())
                        .build());

        entity.setName(strategy.getName());
        entity.setDescription(strategy.getDescription());
        entity.setPriority(strategy.getPriority());
        entity.setEnabled(strategy.isEnabled());
        entity.setCooldownMinutes(strategy.getCooldownMinutes());
        entity.setBuyThreshold(strategy.getBuyThreshold());
        entity.setSellThreshold(strategy.getSellThreshold());
        if (strategy.getState() != null) {
            entity.setState(strategy.getState().name());
        } else {
            entity.setState(com.ibtrader.domain.model.strategy.StrategyState.IDLE.name());
        }
        entity.setUpdatedAt(java.time.Instant.now());

        final TradingStrategyEntity savedEntity = strategyJpaRepository.save(entity);

        UUID versionId = strategy.getVersionId() != null ? strategy.getVersionId() : UUID.randomUUID();
        TradingStrategyVersionEntity versionEntity = versionJpaRepository.findById(versionId)
                .orElseGet(() -> TradingStrategyVersionEntity.builder()
                        .id(versionId)
                        .strategyId(savedEntity.getId())
                        .versionNumber(1)
                        .createdAt(java.time.Instant.now())
                        .build());
        
        versionEntity.setRiskProfile(strategy.getRiskProfile());
        versionEntity.setExecutionMode(strategy.getExecutionMode());
        versionJpaRepository.save(versionEntity);

        // Handle Targets
        targetJpaRepository.deleteByStrategyId(savedEntity.getId());
        targetJpaRepository.flush();
        List<StrategyBasketTargetEntity> savedTargets = new ArrayList<>();
        if (strategy.getTargets() != null && !strategy.getTargets().isEmpty()) {
            List<StrategyBasketTargetEntity> targetEntities = strategy.getTargets().stream()
                    .map(t -> StrategyBasketTargetEntity.builder()
                    .strategyId(savedEntity.getId())
                    .symbol(t.getSymbol())
                    .assetClass(t.getAssetClass())
                    .quantity(t.getQuantity())
                    .build()).toList();
            savedTargets = targetJpaRepository.saveAll(targetEntities);
        }

        return strategyMapper.toDomain(savedEntity, versionEntity, savedTargets);
    }

    @Override
    public Optional<TradingStrategy> findById(UUID id) {
        Optional<TradingStrategyEntity> entityOpt = strategyJpaRepository.findById(id);
        if (entityOpt.isEmpty()) return Optional.empty();

        Optional<TradingStrategyVersionEntity> versionOpt = 
                versionJpaRepository.findFirstByStrategyIdOrderByVersionNumberDesc(id);
        if (versionOpt.isEmpty()) return Optional.empty();

        List<StrategyBasketTargetEntity> targets = targetJpaRepository.findByStrategyId(id);
        return Optional.of(strategyMapper.toDomain(entityOpt.get(), versionOpt.get(), targets));
    }

    @Override
    public List<TradingStrategy> findActiveStrategies() {
        List<TradingStrategyEntity> activeEntities = strategyJpaRepository.findByEnabledTrue();
        return mapEntitiesToDomain(activeEntities);
    }

    @Override
    public List<TradingStrategy> findAll() {
        List<TradingStrategyEntity> allEntities = strategyJpaRepository.findAll();
        return mapEntitiesToDomain(allEntities);
    }

    private List<TradingStrategy> mapEntitiesToDomain(List<TradingStrategyEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        // Batch-fetch versions and targets for all strategies instead of querying per-strategy
        // (avoids N+1 queries when listing active/all strategies).
        List<UUID> strategyIds = entities.stream().map(TradingStrategyEntity::getId).toList();

        Map<UUID, TradingStrategyVersionEntity> latestVersionByStrategyId = versionJpaRepository
                .findByStrategyIdIn(strategyIds).stream()
                .collect(Collectors.toMap(
                        TradingStrategyVersionEntity::getStrategyId,
                        version -> version,
                        (a, b) -> a.getVersionNumber() >= b.getVersionNumber() ? a : b));

        Map<UUID, List<StrategyBasketTargetEntity>> targetsByStrategyId = targetJpaRepository
                .findByStrategyIdIn(strategyIds).stream()
                .collect(Collectors.groupingBy(StrategyBasketTargetEntity::getStrategyId));

        List<TradingStrategy> strategies = new ArrayList<>();
        for (TradingStrategyEntity entity : entities) {
            TradingStrategyVersionEntity version = latestVersionByStrategyId.get(entity.getId());
            if (version != null) {
                List<StrategyBasketTargetEntity> targets =
                        targetsByStrategyId.getOrDefault(entity.getId(), List.of());
                strategies.add(strategyMapper.toDomain(entity, version, targets));
            }
        }
        return strategies;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        targetJpaRepository.deleteByStrategyId(id);
        strategyJpaRepository.deleteById(id);
    }
}
