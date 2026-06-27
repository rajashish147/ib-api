package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetJpaRepository extends JpaRepository<AssetEntity, UUID> {
    List<AssetEntity> findBySymbol(String symbol);
    Optional<AssetEntity> findByIbConId(Integer ibConId);
}
