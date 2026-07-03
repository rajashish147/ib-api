package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.infrastructure.persistence.repository.AssetJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@Profile("!demo")
@RequiredArgsConstructor
public class AssetRepositoryAdapter implements AssetRepository {

    private final AssetJpaRepository jpaRepository;

    @Override
    public Optional<Asset> findById(UUID id) { 
        return jpaRepository.findById(id).map(this::toDomain); 
    }

    @Override
    public Optional<Asset> findBySymbol(String symbol) { 
        List<com.ibtrader.infrastructure.persistence.entity.AssetEntity> assets = jpaRepository.findBySymbol(symbol);
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        // If there are multiple (different exchanges), we just return the first one for now
        return Optional.of(toDomain(assets.get(0)));
    }

    @Override
    public Optional<Asset> findByIbConId(int conId) { 
        return jpaRepository.findByIbConId(conId).map(this::toDomain); 
    }

    @Override
    public List<Asset> findAllEnabled() { 
        return jpaRepository.findAll().stream()
                .filter(com.ibtrader.infrastructure.persistence.entity.AssetEntity::isEnabled)
                .map(this::toDomain)
                .toList(); 
    }

    @Override
    public List<Asset> findAll() { 
        return jpaRepository.findAll().stream()
                .map(this::toDomain)
                .toList(); 
    }

    @Override
    public Asset save(Asset asset) { 
        com.ibtrader.infrastructure.persistence.entity.AssetEntity entity = toEntity(asset);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private Asset toDomain(com.ibtrader.infrastructure.persistence.entity.AssetEntity entity) {
        Asset asset = Asset.builder()
                .id(entity.getId())
                .symbol(entity.getSymbol())
                .exchange(entity.getExchange())
                .currency(entity.getCurrency())
                .assetClass(com.ibtrader.domain.model.asset.AssetClass.valueOf(entity.getAssetClass()))
                .ibConId(entity.getIbConId())
                .multiplier(entity.getMultiplier())
                .expiryDate(entity.getExpiryDate())
                .localSymbol(entity.getLocalSymbol())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion() != null ? entity.getVersion() : 0L)
                .build();
        return asset;
    }

    private com.ibtrader.infrastructure.persistence.entity.AssetEntity toEntity(Asset asset) {
        return com.ibtrader.infrastructure.persistence.entity.AssetEntity.builder()
                .id(asset.getId())
                .symbol(asset.getSymbol())
                .exchange(asset.getExchange())
                .currency(asset.getCurrency())
                .assetClass(asset.getAssetClass().name())
                .ibConId(asset.getIbConId())
                .multiplier(asset.getMultiplier())
                .expiryDate(asset.getExpiryDate())
                .localSymbol(asset.getLocalSymbol())
                .enabled(asset.isEnabled())
                .createdAt(asset.getCreatedAt())
                .updatedAt(asset.getUpdatedAt())
                .version(asset.getVersion())
                .build();
    }
}
