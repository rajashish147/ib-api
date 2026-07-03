package com.ibtrader.domain.event;

import com.ibtrader.domain.model.asset.AssetClass;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.UUID;

/**
 * Event raised when a new asset is registered in the platform.
 */
@Value
@EqualsAndHashCode(callSuper = true)
public class AssetRegisteredEvent extends DomainEvent {

    String symbol;
    String exchange;
    String currency;
    AssetClass assetClass;

    public AssetRegisteredEvent(UUID aggregateId, String symbol, String exchange, String currency, AssetClass assetClass) {
        super(aggregateId, "Asset");
        this.symbol = symbol;
        this.exchange = exchange;
        this.currency = currency;
        this.assetClass = assetClass;
    }
}
