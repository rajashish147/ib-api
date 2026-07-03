package com.ibtrader.infrastructure.persistence.adapter;

import com.ibtrader.domain.model.market.Watchlist;
import com.ibtrader.domain.model.market.WatchlistSymbol;
import com.ibtrader.domain.port.outbound.WatchlistRepository;
import com.ibtrader.infrastructure.persistence.entity.WatchlistEntity;
import com.ibtrader.infrastructure.persistence.entity.WatchlistSymbolEntity;
import com.ibtrader.infrastructure.persistence.mapper.WatchlistMapper;
import com.ibtrader.infrastructure.persistence.repository.WatchlistJpaRepository;
import com.ibtrader.infrastructure.persistence.repository.WatchlistSymbolJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Outbound adapter for WatchlistRepository.
 * Handles persistence operations for Watchlist aggregate root.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchlistRepositoryAdapter implements WatchlistRepository<Watchlist> {

    private final WatchlistJpaRepository repository;
    private final WatchlistSymbolJpaRepository symbolRepository;
    private final WatchlistMapper mapper;

    @Override
    public Watchlist save(Watchlist watchlist) {
        log.debug("Saving Watchlist with id: {}", watchlist.getId());
        
        WatchlistEntity entity = mapper.toEntity(watchlist);
        WatchlistEntity savedEntity = repository.save(entity);
        
        List<WatchlistSymbolEntity> savedSymbols = Collections.emptyList();
        if (watchlist.getSymbols() != null) {
            // Find existing symbols to handle deletions
            List<WatchlistSymbolEntity> existingSymbols = symbolRepository.findByWatchlistId(savedEntity.getId());
            
            List<UUID> currentSymbolIds = watchlist.getSymbols().stream()
                    .map(WatchlistSymbol::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
            List<WatchlistSymbolEntity> symbolsToDelete = existingSymbols.stream()
                    .filter(s -> !currentSymbolIds.contains(s.getId()))
                    .collect(Collectors.toList());
                    
            if (!symbolsToDelete.isEmpty()) {
                symbolRepository.deleteAll(symbolsToDelete);
            }
            
            // Save new and updated symbols
            List<WatchlistSymbolEntity> symbolEntities = watchlist.getSymbols().stream()
                    .map(s -> {
                        WatchlistSymbolEntity se = mapper.toSymbolEntity(s);
                        se.setWatchlistId(savedEntity.getId()); // ensure foreign key is set
                        return se;
                    })
                    .collect(Collectors.toList());
                    
            savedSymbols = symbolRepository.saveAll(symbolEntities);
        }
        
        return mapper.toDomain(savedEntity, savedSymbols);
    }

    @Override
    public Optional<Watchlist> findById(String id) {
        log.debug("Finding Watchlist by id: {}", id);
        UUID uuid = UUID.fromString(id);
        
        return repository.findById(uuid)
                .map(entity -> {
                    List<WatchlistSymbolEntity> symbols = symbolRepository.findByWatchlistId(uuid);
                    return mapper.toDomain(entity, symbols);
                });
    }

    @Override
    public List<Watchlist> findAll() {
        log.debug("Finding all Watchlists");
        return repository.findAll().stream()
                .map(entity -> {
                    List<WatchlistSymbolEntity> symbols = symbolRepository.findByWatchlistId(entity.getId());
                    return mapper.toDomain(entity, symbols);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        log.debug("Deleting Watchlist by id: {}", id);
        UUID uuid = UUID.fromString(id);
        
        // Delete child symbols first
        List<WatchlistSymbolEntity> existingSymbols = symbolRepository.findByWatchlistId(uuid);
        if (!existingSymbols.isEmpty()) {
            symbolRepository.deleteAll(existingSymbols);
        }
        
        // Delete aggregate root
        repository.deleteById(uuid);
    }
}
