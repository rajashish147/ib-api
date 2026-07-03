package com.ibtrader.infrastructure.ibkr.adapter;

import com.ibtrader.domain.port.outbound.AccountDataPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IbkrAccountDataAdapter implements AccountDataPort {

    @Override
    public void requestAccountUpdates(String accountId) {
        log.info("Requesting account updates for {}", accountId);
    }

    @Override
    public void requestPositions() {
        log.info("Requesting positions from IB");
    }

    @Override
    public void requestExecutions() {
        log.info("Requesting executions from IB");
    }

    @Override
    public boolean isAccountSyncActive() {
        return true;
    }
}
