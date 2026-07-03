package com.ibtrader.infrastructure.broker.ibkr;

import com.ibtrader.infrastructure.ibkr.OutboxWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Boundary for the command outbox processor.
 */
@Slf4j
@Component
@Profile("!demo")
public class IbCommandOutboxPublisher {

    private final OutboxWorker outboxWorker;

    public IbCommandOutboxPublisher(OutboxWorker outboxWorker) {
        this.outboxWorker = outboxWorker;
    }

    public void publishPendingCommands() {
        log.debug("Publishing pending IB command outbox entries");
        outboxWorker.processOutbox();
    }
}
