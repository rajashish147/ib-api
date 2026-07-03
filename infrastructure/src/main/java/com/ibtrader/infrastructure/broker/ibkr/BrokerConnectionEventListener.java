package com.ibtrader.infrastructure.broker.ibkr;

import com.ibtrader.domain.event.BrokerDisconnectedEvent;
import com.ibtrader.infrastructure.persistence.entity.IbCommandOutboxEntity;
import com.ibtrader.infrastructure.persistence.entity.IbCommandStatus;
import com.ibtrader.infrastructure.persistence.repository.IbCommandOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerConnectionEventListener {

    private final IbCommandOutboxJpaRepository outboxRepository;

    @EventListener
    @Transactional
    public void onBrokerDisconnected(BrokerDisconnectedEvent event) {
        log.warn("Broker disconnected! Aborting any pending or processing trades to prevent stale executions upon reconnect.");
        
        List<IbCommandOutboxEntity> pendingCommands = outboxRepository.findByStatusIn(
                List.of(IbCommandStatus.PENDING, IbCommandStatus.PROCESSING)
        );

        if (!pendingCommands.isEmpty()) {
            pendingCommands.forEach(cmd -> {
                cmd.setStatus(IbCommandStatus.FAILED);
                cmd.setErrorMessage("Aborted due to broker disconnect event: " + event.getReason());
            });
            outboxRepository.saveAll(pendingCommands);
            log.info("Aborted {} pending outbox commands.", pendingCommands.size());
        }
    }
}
