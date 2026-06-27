package com.ibtrader.domain.port.outbound;

import com.ibtrader.domain.model.strategy.AllocationTarget;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port (repository) for {@link AllocationTarget} persistence.
 *
 * <p>{@code AllocationTarget} entities represent the desired percentage allocation
 * of a portfolio to a specific asset within a given strategy.  They are always
 * queried and managed in the context of their owning {@code StrategyInstance}.</p>
 */
public interface AllocationTargetRepository {

    /**
     * Retrieves all allocation targets configured for the specified strategy.
     *
     * @param strategyId the domain UUID of the owning strategy; must not be {@code null}
     * @return a (possibly empty) list of allocation targets; never {@code null}
     */
    List<AllocationTarget> findByStrategyId(UUID strategyId);

    /**
     * Persists a new or updated {@link AllocationTarget}.
     *
     * @param target the allocation target to persist; must not be {@code null}
     * @return the persisted allocation target, potentially with infrastructure-enriched fields
     */
    AllocationTarget save(AllocationTarget target);

    /**
     * Removes all allocation targets belonging to the specified strategy.
     *
     * <p>This bulk-delete is used when the strategy's allocation configuration is
     * completely replaced (delete-all-then-re-insert pattern).</p>
     *
     * @param strategyId the domain UUID of the strategy whose targets should be removed;
     *                   must not be {@code null}
     */
    void deleteByStrategyId(UUID strategyId);

    /**
     * Removes a single allocation target by its domain UUID.
     *
     * @param id the domain UUID of the allocation target to delete; must not be {@code null}
     */
    void deleteById(UUID id);
}
