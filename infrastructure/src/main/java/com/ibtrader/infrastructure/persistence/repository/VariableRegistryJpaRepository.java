package com.ibtrader.infrastructure.persistence.repository;

import com.ibtrader.infrastructure.persistence.entity.VariableRegistryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VariableRegistryJpaRepository extends JpaRepository<VariableRegistryEntity, UUID> {
    Optional<VariableRegistryEntity> findByVariableName(String variableName);
}
