package com.ibtrader.domain.model.order;

import com.ibtrader.domain.model.common.Money;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a single fill record (execution report) received from
 * Interactive Brokers for a given order.
 *
 * <p>IB may send multiple execution reports for a single order when it is
 * filled in multiple tranches across time or across exchanges. Each tranche
 * produces one {@code Execution} record identified by IB's globally unique
 * {@code execId} string.
 *
 * <p>{@code Execution} is immutable after construction. Use the
 * {@link #create} factory method to build instances. The Lombok builder is
 * retained for ORM / mapping frameworks that require no-arg or builder
 * construction.
 *
 * <p>Executions are child entities of the {@link Order} aggregate but may
 * also be stored in a dedicated executions collection for audit and
 * commission reporting purposes.
 */
@Getter
@Builder
@EqualsAndHashCode(of = "id")
public class Execution {

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    /** Internal surrogate key — stable and unique across the system. */
    private final UUID id;

    /** UUID of the internal {@link Order} that this fill belongs to. */
    private final UUID orderId;

    /** IB's numeric order ID corresponding to the filled order. */
    private final Integer ibOrderId;

    /**
     * IB's execution ID string — globally unique per fill event as assigned
     * by the IB matching engine. Used for deduplication.
     */
    private final String execId;

    // -------------------------------------------------------------------------
    // Context
    // -------------------------------------------------------------------------

    /** IB account that received the fill. */
    private final String accountId;

    /** UUID of the asset (contract) that was traded. */
    private final UUID assetId;

    /** Denormalised ticker symbol for display / reporting. */
    private final String symbol;

    // -------------------------------------------------------------------------
    // Fill details
    // -------------------------------------------------------------------------

    /** Direction of the fill ({@link OrderSide#BUY} or {@link OrderSide#SELL}). */
    private final OrderSide side;

    /** Number of units transacted in this fill. */
    private final BigDecimal quantity;

    /** Per-unit execution price. */
    private final Money price;

    /**
     * Commission charged by IB for this execution. May be zero for accounts
     * with fixed-commission tiers.
     */
    private final Money commission;

    /**
     * Realised P&amp;L attributed to this fill by IB. Populated for
     * closing trades; may be zero or null for opening trades.
     */
    private final Money realizedPnL;

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    /** Wall-clock time reported by IB for when the fill occurred. */
    private final Instant executedAt;

    /**
     * Exchange on which the fill was matched, e.g. {@code "NYSE"},
     * {@code "CME"}, {@code "GLOBEX"}.
     */
    private final String exchange;

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Creates a fully-populated {@code Execution} record.
     *
     * @param orderId     internal order UUID (not null)
     * @param ibOrderId   IB numeric order ID (positive)
     * @param execId      IB execution ID string (not blank)
     * @param accountId   IB account identifier (not blank)
     * @param assetId     asset UUID (not null)
     * @param symbol      ticker symbol (not blank)
     * @param side        trade direction (not null)
     * @param quantity    fill quantity — must be positive
     * @param price       fill price per unit (not null)
     * @param commission  broker commission for this fill (not null)
     * @param realizedPnL realised P&amp;L from IB (may be null for opening trades)
     * @param executedAt  IB-reported fill timestamp (not null)
     * @param exchange    exchange on which the fill occurred (not blank)
     * @return an immutable {@code Execution} instance
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public static Execution create(
            UUID orderId,
            Integer ibOrderId,
            String execId,
            String accountId,
            UUID assetId,
            String symbol,
            OrderSide side,
            BigDecimal quantity,
            Money price,
            Money commission,
            Money realizedPnL,
            Instant executedAt,
            String exchange) {

        if (orderId == null)  throw new IllegalArgumentException("orderId must not be null");
        if (ibOrderId == null || ibOrderId <= 0)
            throw new IllegalArgumentException("ibOrderId must be a positive integer");
        if (execId == null || execId.isBlank())
            throw new IllegalArgumentException("execId must not be blank");
        if (accountId == null || accountId.isBlank())
            throw new IllegalArgumentException("accountId must not be blank");
        if (assetId == null)  throw new IllegalArgumentException("assetId must not be null");
        if (symbol == null || symbol.isBlank())
            throw new IllegalArgumentException("symbol must not be blank");
        if (side == null)     throw new IllegalArgumentException("side must not be null");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("quantity must be positive");
        if (price == null)    throw new IllegalArgumentException("price must not be null");
        if (commission == null) throw new IllegalArgumentException("commission must not be null");
        if (executedAt == null) throw new IllegalArgumentException("executedAt must not be null");
        if (exchange == null || exchange.isBlank())
            throw new IllegalArgumentException("exchange must not be blank");

        return Execution.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .ibOrderId(ibOrderId)
                .execId(execId)
                .accountId(accountId)
                .assetId(assetId)
                .symbol(symbol.toUpperCase())
                .side(side)
                .quantity(quantity)
                .price(price)
                .commission(commission)
                .realizedPnL(realizedPnL)
                .executedAt(executedAt)
                .exchange(exchange.toUpperCase())
                .build();
    }
}
