package com.ibtrader.domain.model.strategy;

import com.ibtrader.domain.model.common.Money;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate root representing a configured, stateful trading strategy instance.
 *
 * <p>A {@code StrategyInstance} encapsulates the configuration (thresholds,
 * mode, type) and the current runtime state of a rebalancing strategy.  Its
 * state machine governs when orders are allowed to be generated and submitted.
 *
 * <h2>State machine</h2>
 * <pre>
 *   IDLE → BUY_TRIGGERED | SELL_TRIGGERED
 *   BUY_TRIGGERED  → BUY_EXECUTING  | IDLE | ERROR
 *   BUY_EXECUTING  → BUY_COMPLETED  | ERROR
 *   BUY_COMPLETED  → IDLE           | ERROR
 *   SELL_TRIGGERED → SELL_EXECUTING | IDLE | ERROR
 *   SELL_EXECUTING → SELL_COMPLETED | ERROR
 *   SELL_COMPLETED → IDLE           | ERROR
 *   ERROR          → RECOVERY       | IDLE
 *   RECOVERY       → IDLE           | ERROR
 * </pre>
 *
 * <p>Illegal transitions throw {@link IllegalStateException} to surface
 * programming errors immediately rather than silently corrupting state.
 *
 * <p>Use the {@link #create} factory method; the Lombok-generated builder is
 * reserved for ORM / mapping frameworks.
 *
 * <p>Thread-safety: this class is <em>not</em> thread-safe.
 */
@Getter
@EqualsAndHashCode(of = "id")
public class StrategyInstance {

    // =========================================================================
    // Valid state transition map
    // =========================================================================

    /** Defines the legal state transitions for this strategy's state machine. */
    private static final Map<StrategyState, Set<StrategyState>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new EnumMap<>(StrategyState.class);
        VALID_TRANSITIONS.put(StrategyState.IDLE,
                EnumSet.of(StrategyState.BUY_TRIGGERED, StrategyState.SELL_TRIGGERED));
        VALID_TRANSITIONS.put(StrategyState.BUY_TRIGGERED,
                EnumSet.of(StrategyState.BUY_EXECUTING, StrategyState.IDLE, StrategyState.ERROR));
        VALID_TRANSITIONS.put(StrategyState.BUY_EXECUTING,
                EnumSet.of(StrategyState.BUY_COMPLETED, StrategyState.ERROR));
        VALID_TRANSITIONS.put(StrategyState.BUY_COMPLETED,
                EnumSet.of(StrategyState.IDLE, StrategyState.ERROR));
        VALID_TRANSITIONS.put(StrategyState.SELL_TRIGGERED,
                EnumSet.of(StrategyState.SELL_EXECUTING, StrategyState.IDLE, StrategyState.ERROR));
        VALID_TRANSITIONS.put(StrategyState.SELL_EXECUTING,
                EnumSet.of(StrategyState.SELL_COMPLETED, StrategyState.ERROR));
        VALID_TRANSITIONS.put(StrategyState.SELL_COMPLETED,
                EnumSet.of(StrategyState.IDLE, StrategyState.ERROR));
        VALID_TRANSITIONS.put(StrategyState.ERROR,
                EnumSet.of(StrategyState.RECOVERY, StrategyState.IDLE));
        VALID_TRANSITIONS.put(StrategyState.RECOVERY,
                EnumSet.of(StrategyState.IDLE, StrategyState.ERROR));
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Surrogate key stable across the lifetime of this strategy. */
    private final UUID id;

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** Human-readable display name for the strategy. */
    private final String name;

    /** Broad classification of the strategy's behaviour. */
    private final StrategyType type;

    /** Execution mode determining how trade sizes are calculated. */
    private final StrategyMode mode;

    /**
     * Net liquidation value (NLV) below which the strategy raises a buy
     * signal. When the portfolio NLV drops below this threshold the strategy
     * should deploy capital.
     */
    private Money buyThreshold;

    /**
     * Net liquidation value (NLV) above which the strategy raises a sell
     * signal. When the portfolio NLV rises above this threshold the strategy
     * should reduce exposure.
     */
    private Money sellThreshold;

    /**
     * Fixed cash amount (USD) to deploy per asset in {@link StrategyMode#FIXED_AMOUNT}
     * mode. Ignored in {@link StrategyMode#FULL_REBALANCE} mode.
     */
    private BigDecimal fixedAmountPerAsset;

    // -------------------------------------------------------------------------
    // Operational flags
    // -------------------------------------------------------------------------

    /**
     * Master enable flag. When {@code false} the strategy will not generate
     * any triggers or orders.
     */
    private boolean enabled;

    /**
     * Operator-controlled pause flag. A paused strategy is still enabled but
     * will not generate new triggers. Useful for temporary suspension without
     * full disable/re-enable.
     */
    private boolean paused;

    // -------------------------------------------------------------------------
    // Runtime state
    // -------------------------------------------------------------------------

    /** Current position in the strategy state machine. */
    private StrategyState state;

    /** Timestamp of the most recent trigger evaluation that raised a signal. */
    private Instant lastTriggeredAt;

    // -------------------------------------------------------------------------
    // Audit / locking
    // -------------------------------------------------------------------------

    /** Wall-clock time at which this instance was first persisted. */
    private final Instant createdAt;

    /** Wall-clock time of the most recent mutation. */
    private Instant updatedAt;

    /**
     * Optimistic concurrency version — incremented by the repository on every
     * successful write.
     */
    private final long version;

    // =========================================================================
    // Private constructor
    // =========================================================================

    private StrategyInstance(
            UUID id,
            String name,
            StrategyType type,
            StrategyMode mode,
            StrategyState state,
            Money buyThreshold,
            Money sellThreshold,
            BigDecimal fixedAmountPerAsset,
            boolean enabled,
            boolean paused,
            Instant lastTriggeredAt,
            Instant createdAt,
            Instant updatedAt,
            long version) {

        this.id                 = id;
        this.name               = name;
        this.type               = type;
        this.mode               = mode;
        this.state              = state;
        this.buyThreshold       = buyThreshold;
        this.sellThreshold      = sellThreshold;
        this.fixedAmountPerAsset = fixedAmountPerAsset;
        this.enabled            = enabled;
        this.paused             = paused;
        this.lastTriggeredAt    = lastTriggeredAt;
        this.createdAt          = createdAt;
        this.updatedAt          = updatedAt;
        this.version            = version;
    }

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a new, enabled, idle {@code StrategyInstance} with the supplied
     * configuration.
     *
     * @param name          non-blank display name
     * @param type          strategy type classification
     * @param mode          execution mode
     * @param buyThreshold  NLV level below which a buy signal is raised (not null)
     * @param sellThreshold NLV level above which a sell signal is raised (not null)
     * @return a fresh strategy instance in {@link StrategyState#IDLE} state
     * @throws IllegalArgumentException for invalid arguments
     */
    public static StrategyInstance create(
            String name,
            StrategyType type,
            StrategyMode mode,
            Money buyThreshold,
            Money sellThreshold) {

        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Strategy name must not be blank");
        if (type == null)           throw new IllegalArgumentException("StrategyType must not be null");
        if (mode == null)           throw new IllegalArgumentException("StrategyMode must not be null");
        if (buyThreshold == null)   throw new IllegalArgumentException("buyThreshold must not be null");
        if (sellThreshold == null)  throw new IllegalArgumentException("sellThreshold must not be null");
        if (buyThreshold.getAmount().compareTo(sellThreshold.getAmount()) >= 0) {
            throw new IllegalArgumentException(
                    "buyThreshold must be strictly less than sellThreshold");
        }

        Instant now = Instant.now();
        return new StrategyInstance(
                UUID.randomUUID(),
                name,
                type,
                mode,
                StrategyState.IDLE,
                buyThreshold,
                sellThreshold,
                null,
                true,
                false,
                null,
                now,
                now,
                0L);
    }

    /**
     * Restores a persisted strategy instance without generating new state.
     */
    public static StrategyInstance rehydrate(
            UUID id,
            String name,
            StrategyType type,
            StrategyMode mode,
            StrategyState state,
            Money buyThreshold,
            Money sellThreshold,
            BigDecimal fixedAmountPerAsset,
            boolean enabled,
            boolean paused,
            Instant lastTriggeredAt,
            Instant createdAt,
            Instant updatedAt,
            long version) {

        if (id == null || name == null || name.isBlank() || type == null || mode == null
                || state == null || buyThreshold == null || sellThreshold == null
                || createdAt == null || updatedAt == null || version < 0) {
            throw new IllegalArgumentException("Persisted strategy state is incomplete");
        }
        if (buyThreshold.getAmount().compareTo(sellThreshold.getAmount()) >= 0) {
            throw new IllegalArgumentException(
                    "buyThreshold must be strictly less than sellThreshold");
        }

        return new StrategyInstance(
                id, name, type, mode, state, buyThreshold, sellThreshold,
                fixedAmountPerAsset, enabled, paused, lastTriggeredAt,
                createdAt, updatedAt, version);
    }

    // =========================================================================
    // Business predicates
    // =========================================================================

    /**
     * Evaluates whether the strategy should raise a buy signal given the
     * supplied current portfolio NLV.
     *
     * @param currentNlv the portfolio's current net liquidation value
     * @return {@code true} iff the strategy is active and NLV is below the
     *         buy threshold
     */
    public boolean shouldBuy(Money currentNlv) {
        if (!isActive() || currentNlv == null) return false;
        return currentNlv.getAmount().compareTo(buyThreshold.getAmount()) < 0;
    }

    /**
     * Evaluates whether the strategy should raise a sell signal given the
     * supplied current portfolio NLV.
     *
     * @param currentNlv the portfolio's current net liquidation value
     * @return {@code true} iff the strategy is active and NLV is above the
     *         sell threshold
     */
    public boolean shouldSell(Money currentNlv) {
        if (!isActive() || currentNlv == null) return false;
        return currentNlv.getAmount().compareTo(sellThreshold.getAmount()) > 0;
    }

    /**
     * Returns {@code true} when the strategy is capable of generating and
     * submitting orders.  A strategy is active only when it is enabled, not
     * paused, and not in an error state.
     *
     * @return {@code true} iff {@code enabled && !paused && state != ERROR}
     */
    public boolean isActive() {
        return enabled && !paused && state != StrategyState.ERROR;
    }

    // =========================================================================
    // State machine
    // =========================================================================

    /**
     * Transitions the strategy to {@code newState} if the transition is legal.
     * Updates {@code updatedAt} on every successful transition.
     *
     * @param newState target state (not null)
     * @throws IllegalArgumentException if {@code newState} is null
     * @throws IllegalStateException    if the transition from the current state
     *                                  to {@code newState} is not permitted
     */
    public void transitionTo(StrategyState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("Target state must not be null");
        }
        Set<StrategyState> allowed = VALID_TRANSITIONS.get(this.state);
        if (allowed == null || !allowed.contains(newState)) {
            throw new IllegalStateException(
                    "Invalid state transition: " + this.state + " -> " + newState
                    + ". Allowed targets: " + (allowed != null ? allowed : "none"));
        }
        this.state = newState;
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // Operational controls
    // =========================================================================

    /**
     * Pauses this strategy. A paused strategy will not generate new triggers
     * until {@link #resume()} is called.
     */
    public void pause() {
        this.paused    = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Resumes a previously paused strategy, re-enabling trigger evaluation.
     */
    public void resume() {
        this.paused    = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Enables this strategy so that it participates in trigger evaluation and
     * order generation.
     */
    public void enable() {
        this.enabled   = true;
        this.updatedAt = Instant.now();
    }

    /**
     * Disables this strategy. A disabled strategy will not generate triggers
     * or orders regardless of market conditions.
     */
    public void disable() {
        this.enabled   = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Records the current time as the most recent trigger timestamp. Should be
     * called whenever the strategy evaluator detects a buy or sell signal.
     */
    public void recordTrigger() {
        this.lastTriggeredAt = Instant.now();
        this.updatedAt       = Instant.now();
    }
}
