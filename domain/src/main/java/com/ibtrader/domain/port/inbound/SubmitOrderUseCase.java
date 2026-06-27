package com.ibtrader.domain.port.inbound;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.order.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound port (use case) for submitting a new trading order to Interactive Brokers.
 *
 * <p>The use case validates the order parameters, evaluates applicable risk limits,
 * persists the order aggregate in {@code PENDING} state, submits it to IB via the
 * {@link com.ibtrader.domain.port.outbound.OrderSubmissionPort}, and publishes an
 * {@link com.ibtrader.domain.event.OrderSubmittedEvent} upon success.</p>
 *
 * <p>Use the inner {@link Command} record to pass order parameters.</p>
 */
public interface SubmitOrderUseCase {

    /**
     * Encapsulates all parameters required to submit an order.
     *
     * @param accountId   the IB account to trade in; must not be blank
     * @param assetId     the domain UUID of the asset to trade; must not be {@code null}
     * @param orderType   the order type (e.g. {@code MKT}, {@code LMT}); must not be {@code null}
     * @param side        the order direction ({@code BUY} or {@code SELL}); must not be {@code null}
     * @param quantity    the quantity to trade in shares or contract units; must be positive
     * @param limitPrice  the limit price for {@code LMT} orders; {@code null} for {@code MKT} orders
     * @param stopPrice   the stop price for stop orders; {@code null} when not applicable
     * @param strategyRef an opaque reference to the strategy or plan that generated this order;
     *                    may be {@code null} for manually submitted orders
     */
    record Command(
            String accountId,
            UUID assetId,
            OrderType orderType,
            OrderSide side,
            BigDecimal quantity,
            Money limitPrice,
            Money stopPrice,
            String strategyRef
    ) {}

    /**
     * Executes the order submission use case.
     *
     * @param command the order command containing all required parameters; must not be {@code null}
     * @return the persisted {@link Order} aggregate in {@code SUBMITTED} state
     * @throws com.ibtrader.domain.exception.AssetNotFoundException       if the asset is not registered
     * @throws com.ibtrader.domain.exception.RiskLimitViolatedException   if a risk limit is breached
     * @throws com.ibtrader.domain.exception.InsufficientFundsException   if buying power is insufficient
     * @throws com.ibtrader.domain.exception.BrokerConnectionException        if IB is not reachable
     */
    Order execute(Command command);
}
