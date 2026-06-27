package com.ibtrader.infrastructure.broker.ibkr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Boundary for the command outbox processor.
 *
 * <p>The persistence implementation is intentionally deferred; this component
 * provides the startup-safe collaboration point without submitting commands
 * directly or introducing trading behavior.</p>
 */
@Slf4j
@Component
public class IbCommandOutboxPublisher {

    public void publishPendingCommands() {
        log.debug("IB command outbox publisher is idle; no persistence adapter is configured");
    }
}
