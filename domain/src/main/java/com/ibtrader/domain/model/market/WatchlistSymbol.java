package com.ibtrader.domain.model.market;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@ToString
public class WatchlistSymbol {
    private final UUID id;
    private final UUID watchlistId;
    private final String symbol;
    private final String sector;
    private final String industry;
    private final String tags;
    private final BigDecimal allocationLimit;
    private final String notes;
    private final boolean enabled;
    private final Instant createdAt;
}
