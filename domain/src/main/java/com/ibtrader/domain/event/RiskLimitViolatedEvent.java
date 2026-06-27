package com.ibtrader.domain.event;

import com.ibtrader.domain.model.risk.LimitType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised when the risk management subsystem detects a violation of a
 * configured {@link LimitType} threshold, potentially blocking order submission.
 *
 * <p>Consumers — including operator alert channels, circuit-breaker services, and
 * audit log writers — should subscribe to this event to ensure appropriate
 * corrective action is taken promptly.</p>
 *
 * <p>{@code blockedOrderId} is populated when the violation caused a specific order
 * to be blocked and is {@code null} when the violation was detected independently
 * of an in-flight order (e.g. during a scheduled portfolio health check).</p>
 */
@Getter
public final class RiskLimitViolatedEvent extends DomainEvent {

    /**
     * The domain identifier of the {@code StrategyInstance} context in which the
     * violation was detected.
     */
    private final UUID strategyId;

    /**
     * The category of risk limit that was violated (e.g. {@code MAX_POSITION_SIZE},
     * {@code DAILY_LOSS}).
     */
    private final LimitType limitType;

    /**
     * The configured limit value that was exceeded or breached.
     */
    private final BigDecimal limitValue;

    /**
     * The actual measured value that caused the violation.
     */
    private final BigDecimal actualValue;

    /**
     * A human-readable explanation of the violation, suitable for operator dashboards
     * and alert messages.
     */
    private final String violationMessage;

    /**
     * The domain identifier of the order that was blocked as a result of this violation.
     * May be {@code null} if the violation was not triggered by an order attempt.
     */
    private final UUID blockedOrderId;

    /**
     * Constructs a {@code RiskLimitViolatedEvent} via its Lombok builder.
     *
     * @param strategyId       domain identifier of the strategy in which the violation was detected
     * @param limitType        the type of risk limit that was breached
     * @param limitValue       the configured threshold value
     * @param actualValue      the actual measured value
     * @param violationMessage human-readable description of the violation
     * @param blockedOrderId   domain identifier of a blocked order, or {@code null}
     * @param sequenceNumber   monotonic sequence number scoped to the strategy aggregate
     */
    @Builder
    private RiskLimitViolatedEvent(
            UUID strategyId,
            LimitType limitType,
            BigDecimal limitValue,
            BigDecimal actualValue,
            String violationMessage,
            UUID blockedOrderId,
            long sequenceNumber) {

        super(strategyId, "StrategyInstance", sequenceNumber);
        this.strategyId        = strategyId;
        this.limitType         = limitType;
        this.limitValue        = limitValue;
        this.actualValue       = actualValue;
        this.violationMessage  = violationMessage;
        this.blockedOrderId    = blockedOrderId;
    }
}
