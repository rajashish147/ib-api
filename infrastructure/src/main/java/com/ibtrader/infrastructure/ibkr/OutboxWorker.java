package com.ibtrader.infrastructure.ibkr;

import com.ibtrader.infrastructure.broker.ibkr.IbConnectionManager;
import com.ibtrader.infrastructure.persistence.entity.IbCommandOutboxEntity;
import com.ibtrader.infrastructure.persistence.entity.IbCommandStatus;
import com.ibtrader.infrastructure.persistence.repository.IbCommandOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Dedicated background worker responsible for reading pending orders from the 
 * Outbox table, sending them to Interactive Brokers, and managing retries.
 *
 * <p>It isolates the execution of commands from the strategy evaluation pipeline.</p>
 */
@Component
@Profile("!demo")
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

        List<IbCommandOutboxEntity> pendingCommands = outboxRepository
                .findByStatusInAndNextRetryAtLessThanEqual(
                        Arrays.asList(IbCommandStatus.PENDING, IbCommandStatus.FAILED),
                        Instant.now());

        if (pendingCommands.isEmpty()) {
            return;
        }

        LOG.info(String.format("Found %d pending outbox commands. Processing...", pendingCommands.size()));

        for (IbCommandOutboxEntity command : pendingCommands) {
            try {
                // 2. Mark as processing
                command.setStatus(IbCommandStatus.PROCESSING);
                command.setLastAttemptAt(Instant.now());
                command.setUpdatedAt(Instant.now());
                outboxRepository.saveAndFlush(command);

                // 3. Send to IBKR
                // Since IbConnectionManager uses EClientSocket asynchronously, this submits the request
                // and the execution report will be picked up by the IB Wrapper callbacks later.
                ibConnectionManager.submitCommand(command.getPayload());

                // 4. Update status to SUBMITTED
                command.setStatus(IbCommandStatus.SENT);
                command.setSentAt(Instant.now());
                command.setUpdatedAt(Instant.now());
                outboxRepository.save(command);
                LOG.info(String.format("Successfully submitted command %s via IB API.", command.getId()));

            } catch (Exception e) {
                LOG.severe(String.format("Failed to submit outbox command %s: %s", command.getId(), e.getMessage()));
                
                // 5. Retry logic
                int attempts = command.getAttemptCount() == null ? 1 : command.getAttemptCount() + 1;
                command.setAttemptCount(attempts);

                int maxAttempts = command.getMaxAttempts() == null ? 3 : command.getMaxAttempts();
                if (attempts >= maxAttempts) {
                    LOG.severe(String.format("Command %s exceeded max retries. Dead-lettering.", command.getId()));
                    command.setStatus(IbCommandStatus.SKIPPED);
                    command.setErrorMessage("Max retries exceeded: " + e.getMessage());
                } else {
                    command.setStatus(IbCommandStatus.FAILED); 
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
