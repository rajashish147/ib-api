package com.ibtrader.infrastructure.ibkr;

import com.ibtrader.infrastructure.persistence.entity.IbCommandOutboxEntity;
import com.ibtrader.infrastructure.persistence.repository.IbCommandOutboxJpaRepository;
import com.ibtrader.infrastructure.broker.ibkr.IbConnectionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * Dedicated background worker responsible for reading pending orders from the 
 * Outbox table, sending them to Interactive Brokers, and managing retries.
 *
 * <p>It isolates the execution of commands from the strategy evaluation pipeline.</p>
 */
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private static final Logger LOG = Logger.getLogger(OutboxWorker.class.getName());

    private final IbCommandOutboxJpaRepository outboxRepository;
    private final IbConnectionManager ibConnectionManager;

    /**
     * Polls the outbox for pending commands every 1 second.
     */
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutbox() {
        if (!ibConnectionManager.isConnected()) {
            // Cannot process outbox if not connected to IBKR
            return;
        }

        // Note: For production, we'd use a repository method with pessimistic write lock
        List<IbCommandOutboxEntity> pendingCommands = outboxRepository
                .findByStatusAndNextRetryAtLessThanEqual("PENDING", Instant.now());

        if (pendingCommands.isEmpty()) {
            return;
        }

        LOG.info(String.format("Found %d pending outbox commands. Processing...", pendingCommands.size()));

        for (IbCommandOutboxEntity command : pendingCommands) {
            try {
                // 2. Mark as processing
                command.setStatus("PROCESSING");
                command.setUpdatedAt(Instant.now());
                outboxRepository.saveAndFlush(command);

                // 3. Send to IBKR
                // Since IbConnectionManager uses EClientSocket asynchronously, this submits the request
                // and the execution report will be picked up by the IB Wrapper callbacks later.
                ibConnectionManager.submitCommand(command.getPayload());

                // 4. Update status to SUBMITTED
                command.setStatus("SUBMITTED");
                command.setUpdatedAt(Instant.now());
                outboxRepository.save(command);
                LOG.info(String.format("Successfully submitted command %s via IB API.", command.getId()));

            } catch (Exception e) {
                LOG.severe(String.format("Failed to submit outbox command %s: %s", command.getId(), e.getMessage()));
                
                // 5. Retry logic
                int attempts = command.getAttemptCount() + 1;
                command.setAttemptCount(attempts);
                
                if (attempts >= 3) {
                    LOG.severe(String.format("Command %s exceeded max retries. Dead-lettering.", command.getId()));
                    command.setStatus("DEAD_LETTER");
                    command.setErrorMessage("Max retries exceeded: " + e.getMessage());
                } else {
                    command.setStatus("FAILED"); 
                    command.setErrorMessage(e.getMessage());
                }
                
                command.setNextRetryAt(Instant.now()
                        .plusSeconds((long) Math.pow(2, command.getAttemptCount()) * 10));
                command.setUpdatedAt(Instant.now());
                outboxRepository.save(command);
            }
        }
    }
}
