package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByIbOrderId(Integer ibOrderId);
    List<OrderEntity> findByStatus(String status);
    List<OrderEntity> findByStatusIn(List<String> statuses);
    List<OrderEntity> findByRebalancePlanId(UUID rebalancePlanId);
    List<OrderEntity> findByAssetId(UUID assetId);
}
