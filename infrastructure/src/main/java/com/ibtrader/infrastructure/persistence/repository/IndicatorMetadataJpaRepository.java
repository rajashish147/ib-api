package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.IndicatorMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndicatorMetadataJpaRepository extends JpaRepository<IndicatorMetadataEntity, UUID> {
    Optional<IndicatorMetadataEntity> findByIndicatorName(String indicatorName);
}
