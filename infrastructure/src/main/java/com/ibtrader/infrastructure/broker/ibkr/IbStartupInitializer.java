package com.ibtrader.infrastructure.broker.ibkr;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Starts the optional broker connection after the Spring context is ready.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.ib", name = "enabled", havingValue = "true")
public class IbStartupInitializer {

    private final IbConnectionManager connectionManager;

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        connectionManager.connect();
    }
}
