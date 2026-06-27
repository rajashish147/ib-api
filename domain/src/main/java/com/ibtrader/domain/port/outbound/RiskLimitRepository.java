package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.risk.LimitType;
import com.ibtrader.domain.model.risk.RiskLimit;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port (repository) for {@link RiskLimit} persistence.
 *
 * <p>Risk limits define guardrails (maximum position sizes, daily loss caps, etc.)
 * that are enforced by the risk evaluation service before orders are submitted to IB.
 * Limits are keyed by their {@link LimitType} and may be enabled or disabled
 * independently.</p>
 */
public interface RiskLimitRepository {

    /**
     * Retrieves the risk limit configuration for a specific limit type.
     *
     * @param type the {@link LimitType} to look up; must not be {@code null}
     * @return an {@link Optional} containing the risk limit if one is configured, or
     *         {@link Optional#empty()} if no limit of that type has been defined
     */
    Optional<RiskLimit> findByLimitType(LimitType type);

    /**
     * Retrieves all risk limits that are currently enabled.
     *
     * <p>The risk evaluation service should call this method on each evaluation cycle
     * to retrieve the active guardrail set without loading disabled limits.</p>
     *
     * @return a (possibly empty) list of enabled risk limits; never {@code null}
     */
    List<RiskLimit> findAllEnabled();

    /**
     * Retrieves all configured risk limits, regardless of their enabled state.
     *
     * <p>Used by the administration interface to present the full risk limit
     * configuration to operators.</p>
     *
     * @return a (possibly empty) list of all risk limits; never {@code null}
     */
    List<RiskLimit> findAll();

    /**
     * Persists a new or updated {@link RiskLimit}.
     *
     * @param limit the risk limit to persist; must not be {@code null}
     * @return the persisted risk limit, potentially with infrastructure-enriched fields
     */
    RiskLimit save(RiskLimit limit);
}
