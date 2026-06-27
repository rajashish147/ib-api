package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.risk.LimitType;
import com.ibtrader.domain.model.risk.RiskLimit;

import java.math.BigDecimal;

/**
 * Inbound port (use case) for updating the configured value and enabled state of
 * a {@link RiskLimit}.
 *
 * <p>Risk limits are global guardrails enforced by the risk evaluation service
 * before each order submission.  This use case allows operators to adjust limits
 * at runtime without a deployment, supporting dynamic risk management workflows.</p>
 *
 * <p>If no limit exists for the specified {@link LimitType}, a new one is created.
 * If one already exists, it is updated in place.</p>
 */
public interface UpdateRiskLimitUseCase {

    /**
     * Encapsulates the parameters required to update a risk limit.
     *
     * @param limitType the {@link LimitType} identifying which limit to update;
     *                  must not be {@code null}
     * @param newValue  the new threshold value to apply; must be non-negative
     * @param enabled   {@code true} to activate the limit, {@code false} to disable it
     *                  without deleting the configuration
     */
    record Command(LimitType limitType, BigDecimal newValue, boolean enabled) {}

    /**
     * Executes the risk limit update use case.
     *
     * @param command the update command; must not be {@code null}
     * @return the updated (or newly created) {@link RiskLimit}; never {@code null}
     * @throws com.ibtrader.domain.exception.DomainException if the supplied value
     *         is out of range for the specified limit type
     */
    RiskLimit execute(Command command);
}
