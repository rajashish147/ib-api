package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.IbCommandOutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface IbCommandOutboxJpaRepository extends JpaRepository<IbCommandOutboxEntity, UUID> {
    List<IbCommandOutboxEntity> findByStatusAndNextRetryAtLessThanEqual(String status, Instant nextRetryAt);
}
