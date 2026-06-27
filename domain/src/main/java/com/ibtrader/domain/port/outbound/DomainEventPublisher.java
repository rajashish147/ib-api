package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.event.DomainEvent;

/**
 * Outbound port for publishing {@link DomainEvent} instances to interested consumers.
 *
 * <p>Implementations may route events to an in-process Spring application event bus
 * (via {@code ApplicationEventPublisher}), an external message broker (e.g. Kafka,
 * RabbitMQ), or a combination of both.  The domain layer has no knowledge of the
 * transport mechanism and depends solely on this interface.</p>
 *
 * <p>Publishing is expected to be a fire-and-forget operation from the caller's
 * perspective.  Transactional outbox patterns or at-least-once delivery guarantees
 * are implementation concerns and must not leak into the domain.</p>
 */
public interface DomainEventPublisher {

    /**
     * Publishes the supplied domain event to all registered consumers.
     *
     * <p>Callers should publish events <em>after</em> the aggregate state change has
     * been persisted — ideally at the end of a successful unit of work — to avoid
     * publishing events for uncommitted changes.</p>
     *
     * @param event the domain event to publish; must not be {@code null}
     */
    void publish(DomainEvent event);
}
