package com.ibtrader.domain.port.outbound;

import java.util.List;

public interface IndicatorValuePort<T> {
    T fetchValue(String indicatorId, String symbol);
    List<T> fetchHistoricalValues(String indicatorId, String symbol, String timeframe, int limit);
}
