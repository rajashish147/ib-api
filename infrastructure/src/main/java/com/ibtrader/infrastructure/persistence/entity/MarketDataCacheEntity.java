package com.ibtrader.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_data_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class MarketDataCacheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "asset_id")
    private UUID assetId;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "last_price", precision = 19, scale = 4)
    private BigDecimal lastPrice;

    @Column(name = "bid_price", precision = 19, scale = 4)
    private BigDecimal bidPrice;

    @Column(name = "ask_price", precision = 19, scale = 4)
    private BigDecimal askPrice;

    @Column(name = "open_price", precision = 19, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "high_price", precision = 19, scale = 4)
    private BigDecimal highPrice;

    @Column(name = "low_price", precision = 19, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "close_price", precision = 19, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "volume", precision = 19, scale = 4)
    private BigDecimal volume;

    @Column(name = "currency")
    private String currency;

    @Column(name = "ticker_id")
    private Integer tickerId;

    @Column(name = "price_at")
    private Instant priceAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
