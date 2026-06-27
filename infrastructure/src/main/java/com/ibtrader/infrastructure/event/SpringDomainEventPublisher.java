package com.ibtrader.infrastructure.event;

import com.ibtrader.domain.event.DomainEvent;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events on Spring's in-process event bus.
 */
@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
