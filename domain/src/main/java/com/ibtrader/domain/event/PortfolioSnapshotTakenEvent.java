package com.ibtrader.domain.event;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain event raised whenever the system successfully captures a point-in-time
 * snapshot of a portfolio's financial state.
 *
 * <p>Consumers of this event — such as analytics pipelines, reporting services,
 * and strategy evaluation engines — can use it to track NLV trends, detect
 * threshold crossings, and trigger downstream rebalancing logic.</p>
 *
 * <p>The event is immutable once constructed via its {@link Builder}.</p>
 */
@Getter
public final class PortfolioSnapshotTakenEvent extends DomainEvent {

    /**
     * The domain identifier of the {@code Portfolio} aggregate whose snapshot was taken.
     */
    private final UUID portfolioId;

    /**
     * The IB account identifier (e.g. {@code "DU1234567"}) associated with the portfolio.
     */
    private final String accountId;

    /**
     * Net liquidation value of the portfolio at the moment the snapshot was taken.
     * Expressed in the account's base currency.
     */
    private final BigDecimal netLiquidationValue;

    /**
     * Total cash and cash-equivalent balances held in the account.
     */
    private final BigDecimal totalCashValue;

    /**
     * Aggregate unrealised profit-and-loss across all open positions.
     */
    private final BigDecimal unrealizedPnL;

    /**
     * Aggregate realised profit-and-loss from closed positions within the
     * current trading day or accounting period.
     */
    private final BigDecimal realizedPnL;

    /**
     * Number of distinct positions held in the portfolio at snapshot time.
     */
    private final int positionCount;

    /**
     * Constructs a {@code PortfolioSnapshotTakenEvent}.  Intended to be called
     * exclusively through the Lombok-generated {@link Builder}.
     *
     * @param portfolioId          domain identifier of the portfolio aggregate
     * @param accountId            IB account string (must not be {@code null})
     * @param netLiquidationValue  NLV at snapshot time
     * @param totalCashValue       cash balance at snapshot time
     * @param unrealizedPnL        open P&amp;L at snapshot time
     * @param realizedPnL          closed P&amp;L at snapshot time
     * @param positionCount        number of open positions
     * @param sequenceNumber       monotonic sequence number scoped to the portfolio aggregate
     */
    @Builder
    private PortfolioSnapshotTakenEvent(
            UUID portfolioId,
            String accountId,
            BigDecimal netLiquidationValue,
            BigDecimal totalCashValue,
            BigDecimal unrealizedPnL,
            BigDecimal realizedPnL,
            int positionCount,
            long sequenceNumber) {

        super(portfolioId, "Portfolio", sequenceNumber);
        this.portfolioId           = portfolioId;
        this.accountId             = accountId;
        this.netLiquidationValue   = netLiquidationValue;
        this.totalCashValue        = totalCashValue;
        this.unrealizedPnL         = unrealizedPnL;
        this.realizedPnL           = realizedPnL;
        this.positionCount         = positionCount;
    }
}
