package com.ibtrader.infrastructure.adapter;

import com.ibtrader.domain.port.outbound.IndicatorValuePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Adapter for IndicatorValuePort.
 * In a real implementation, this would connect to an external indicator service or a local database.
 */
@Service
@Slf4j
public class IndicatorValueAdapter implements IndicatorValuePort<Double> {

    @Override
    public Double fetchValue(String indicatorId, String symbol) {
        log.info("Fetching indicator value for id: {} and symbol: {}", indicatorId, symbol);
        // Mock returning a dummy value for now
        return 0.0;
    }

    @Override
    public List<Double> fetchHistoricalValues(String indicatorId, String symbol, String timeframe, int limit) {
        log.info("Fetching historical indicator values for id: {}, symbol: {}, timeframe: {}, limit: {}",
                 indicatorId, symbol, timeframe, limit);
        return Collections.emptyList();
    }
}
