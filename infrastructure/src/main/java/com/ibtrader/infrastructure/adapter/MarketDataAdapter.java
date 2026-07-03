package com.ibtrader.infrastructure.adapter;

import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.port.outbound.MarketDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adapter for MarketDataPort.
 * In a real implementation, this would connect to IbApiClient.
 * For now, this serves as a skeleton/mock implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataAdapter implements MarketDataPort {

    private final AtomicInteger tickerIdGenerator = new AtomicInteger(1000);
    private final ConcurrentHashMap<UUID, Integer> activeSubscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribeToMarketData(Asset asset) {
        if (activeSubscriptions.containsKey(asset.getId())) {
            log.info("Already subscribed to market data for asset: {}", asset.getSymbol());
            return;
        }
        
        int tickerId = tickerIdGenerator.incrementAndGet();
        activeSubscriptions.put(asset.getId(), tickerId);
        log.info("Subscribed to market data for asset: {} with tickerId: {}", asset.getSymbol(), tickerId);
        
        // TODO: Call IbApiClient to actually subscribe
    }

    @Override
    public void unsubscribeFromMarketData(int tickerId) {
        log.info("Unsubscribing from market data tickerId: {}", tickerId);
        
        // TODO: Call IbApiClient to cancel
        activeSubscriptions.values().removeIf(id -> id == tickerId);
    }

    @Override
    public void requestHistoricalData(Asset asset, String duration, String barSize) {
        log.info("Requesting historical data for asset: {}, duration: {}, barSize: {}", 
                 asset.getSymbol(), duration, barSize);
                 
        // TODO: Call IbApiClient for historical data
    }

    @Override
    public boolean isSubscribed(UUID assetId) {
        return activeSubscriptions.containsKey(assetId);
    }
}
