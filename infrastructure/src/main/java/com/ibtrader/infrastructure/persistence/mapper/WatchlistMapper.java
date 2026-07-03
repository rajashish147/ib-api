package com.ibtrader.infrastructure.persistence.mapper;

import com.ibtrader.domain.model.market.Watchlist;
import com.ibtrader.domain.model.market.WatchlistSymbol;
import com.ibtrader.infrastructure.persistence.entity.WatchlistEntity;
import com.ibtrader.infrastructure.persistence.entity.WatchlistSymbolEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WatchlistMapper {

    /**
     * Maps Watchlist domain model to WatchlistEntity.
     */
    public WatchlistEntity toEntity(Watchlist domain) {
        if (domain == null) {
            return null;
        }

        return WatchlistEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .priority(domain.getPriority())
                .enabled(domain.isEnabled())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    /**
     * Maps WatchlistEntity and its symbols to Watchlist domain model.
     */
    public Watchlist toDomain(WatchlistEntity entity, List<WatchlistSymbolEntity> symbolEntities) {
        if (entity == null) {
            return null;
        }

        List<WatchlistSymbol> symbols = symbolEntities == null ? Collections.emptyList() :
                symbolEntities.stream()
                        .map(this::toSymbolDomain)
                        .collect(Collectors.toList());

        return Watchlist.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .symbols(symbols)
                .build();
    }

    /**
     * Maps WatchlistSymbol domain model to WatchlistSymbolEntity.
     */
    public WatchlistSymbolEntity toSymbolEntity(WatchlistSymbol domain) {
        if (domain == null) {
            return null;
        }

        return WatchlistSymbolEntity.builder()
                .id(domain.getId())
                .watchlistId(domain.getWatchlistId())
                .symbol(domain.getSymbol())
                .sector(domain.getSector())
                .industry(domain.getIndustry())
                .tags(domain.getTags())
                .allocationLimit(domain.getAllocationLimit())
                .notes(domain.getNotes())
                .enabled(domain.isEnabled())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * Maps WatchlistSymbolEntity to WatchlistSymbol domain model.
     */
    public WatchlistSymbol toSymbolDomain(WatchlistSymbolEntity entity) {
        if (entity == null) {
            return null;
        }

        return WatchlistSymbol.builder()
                .id(entity.getId())
                .watchlistId(entity.getWatchlistId())
                .symbol(entity.getSymbol())
                .sector(entity.getSector())
                .industry(entity.getIndustry())
                .tags(entity.getTags())
                .allocationLimit(entity.getAllocationLimit())
                .notes(entity.getNotes())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
