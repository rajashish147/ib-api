package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.portfolio.PortfolioSnapshot;
import java.util.List;

public interface GetSnapshotHistoryUseCase {
    record Query(String accountId, int limit) {}
    List<PortfolioSnapshot> execute(Query query);
}
