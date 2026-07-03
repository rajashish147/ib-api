package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.port.outbound.TradeSignalRepository;
import com.ibtrader.infrastructure.persistence.entity.TradeSignalEntity;
import com.ibtrader.infrastructure.persistence.mapper.TradeSignalMapper;
import com.ibtrader.infrastructure.persistence.repository.TradeSignalJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Outbound adapter for TradeSignalRepository.
 * Handles persistence operations for TradeSignal domain objects.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeSignalRepositoryAdapter implements TradeSignalRepository<TradeSignal> {

    private final TradeSignalJpaRepository repository;
    private final TradeSignalMapper mapper;

    @Override
    public TradeSignal save(TradeSignal signal) {
        log.debug("Saving TradeSignal with id: {}", signal.getId());
        TradeSignalEntity entity = mapper.toEntity(signal);
        TradeSignalEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<TradeSignal> findById(String id) {
        log.debug("Finding TradeSignal by id: {}", id);
        return repository.findById(UUID.fromString(id))
                .map(mapper::toDomain);
    }

    @Override
    public List<TradeSignal> findAll() {
        log.debug("Finding all TradeSignals");
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        log.debug("Deleting TradeSignal by id: {}", id);
        repository.deleteById(UUID.fromString(id));
    }
}
