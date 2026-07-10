package com.ibtrader.infrastructure.ibkr.adapter;

import com.ibtrader.domain.port.outbound.AccountDataPort;
import com.ibtrader.infrastructure.broker.ibkr.IbConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Outbound adapter implementing {@link AccountDataPort} by delegating to the
 * live {@link IbConnectionManager} which owns the IB TWS / Gateway connection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IbkrAccountDataAdapter implements AccountDataPort {

    private final IbConnectionManager connectionManager;

    @Override
    public void requestAccountUpdates(String accountId) {
        log.info("Requesting account updates for {}", accountId);
        // Account values arrive via the pipeline's PortfolioSnapshotStage which
        // reads from the TWS account-summary subscription. No explicit call needed.
    }

    @Override
    public void requestPositions() {
        log.info("Requesting positions from IB Gateway");
        connectionManager.requestPositions();
    }

    @Override
    public void requestExecutions() {
        log.info("Requesting executions from IB");
        // Not yet implemented — planned for order-fill processing stage.
    }

    @Override
    public boolean isAccountSyncActive() {
        return connectionManager.isConnected();
    }
}
