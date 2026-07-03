package com.ibtrader.domain.model.market;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@ToString
public class Watchlist {
    private final UUID id;
    private final String name;
    private final String description;
    private final Integer priority;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Long version;
    private final List<WatchlistSymbol> symbols;
}
