package com.ibtrader.application.service;

import com.ibtrader.domain.event.OrderCancelledEvent;
import com.ibtrader.domain.event.OrderSubmittedEvent;
import com.ibtrader.domain.exception.InsufficientFundsException;
import com.ibtrader.domain.exception.InvalidOrderStateException;
import com.ibtrader.domain.exception.RiskLimitViolatedException;
import com.ibtrader.domain.model.asset.Asset;
import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.order.OrderStatus;
import com.ibtrader.domain.model.order.OrderType;
import com.ibtrader.domain.model.risk.LimitType;
import com.ibtrader.domain.port.inbound.CancelOrderUseCase;
import com.ibtrader.domain.port.inbound.SubmitOrderUseCase;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.DomainEventPublisher;
import com.ibtrader.domain.port.outbound.OrderRepository;
import com.ibtrader.domain.port.outbound.OrderSubmissionPort;
import com.ibtrader.domain.port.outbound.PortfolioRepository;
import com.ibtrader.domain.port.outbound.RiskLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Application service implementing inbound use cases for order management.
 * Coordinates domain models and outbound ports to submit and cancel orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements SubmitOrderUseCase, CancelOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderSubmissionPort orderSubmissionPort;
    private final DomainEventPublisher domainEventPublisher;
    private final AssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;
    private final RiskLimitRepository riskLimitRepository;

    /**
     * Executes the order submission use case.
     * Validates parameters, evaluates risk limits, persists the order, submits it to IB,
     * and publishes a domain event.
     *
     * @param command the order command
     * @return the submitted Order aggregate
     */
    @Override
    @Transactional
    public Order execute(SubmitOrderUseCase.Command command) {
        log.info("Executing submit order use case for asset: {}", command.assetId());

        Asset asset = assetRepository.findById(command.assetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        evaluateRiskLimits(command);
        evaluateFunds(command, asset);

        Order order = createOrder(command, asset);

        // Persist order in PENDING_SUBMIT state
        order = orderRepository.save(order);

        // Submit to IB
        int ibOrderId = orderSubmissionPort.submitOrder(order, asset);
        order.assignIbOrderId(ibOrderId);

        order = orderRepository.save(order);

        publishOrderSubmittedEvent(order);

        return order;
    }

    /**
     * Executes the order cancellation use case.
     * Validates the order state, requests cancellation from IB, and publishes a domain event.
     *
     * @param command the cancel command
     * @return the cancelled (or pending cancel) Order aggregate
     */
    @Override
    @Transactional
    public Order execute(CancelOrderUseCase.Command command) {
        log.info("Executing cancel order use case for order: {}", command.orderId());

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.isComplete() && order.getStatus() != OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException(order.getId(), order.getStatus(), null);
        }

        if (order.getIbOrderId() != null) {
            orderSubmissionPort.cancelOrder(order.getIbOrderId());
        }

        order.cancel(command.reason());
        order = orderRepository.save(order);

        publishOrderCancelledEvent(order, command.reason());

        return order;
    }

    private Order createOrder(SubmitOrderUseCase.Command command, Asset asset) {
        if (command.orderType() == OrderType.MARKET) {
            return Order.createMarket(command.accountId(), command.assetId(), asset.getSymbol(),
                    command.side(), command.quantity(), command.strategyRef());
        } else if (command.orderType() == OrderType.LIMIT) {
            return Order.createLimit(command.accountId(), command.assetId(), asset.getSymbol(),
                    command.side(), command.quantity(), command.limitPrice(), command.strategyRef());
        } else if (command.orderType() == OrderType.STOP) {
            return Order.createStop(command.accountId(), command.assetId(), asset.getSymbol(),
                    command.side(), command.quantity(), command.stopPrice(), command.strategyRef());
        } else {
            throw new IllegalArgumentException("Unsupported order type: " + command.orderType());
        }
    }

    private void evaluateRiskLimits(SubmitOrderUseCase.Command command) {
        portfolioRepository.findByAccountId(command.accountId()).ifPresent(portfolio -> {
            Money nlv = portfolio.getNetLiquidationValue();
            if (nlv != null) {
                riskLimitRepository.findAllEnabled().forEach(limit -> {
                    if (limit.getLimitType() == LimitType.EMERGENCY_STOP_NLV) {
                        if (nlv.getAmount().compareTo(limit.getValue()) < 0) {
                            throw RiskLimitViolatedException.of(limit.getLimitType(), limit.getValue(), nlv.getAmount());
                        }
                    }
                });
            }
        });
    }

    private void evaluateFunds(SubmitOrderUseCase.Command command, Asset asset) {
        if (command.side() == OrderSide.BUY && command.limitPrice() != null) {
            portfolioRepository.findByAccountId(command.accountId()).ifPresent(portfolio -> {
                Money buyingPower = portfolio.getBuyingPower();
                if (buyingPower != null && buyingPower.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal notional = command.quantity()
                            .multiply(command.limitPrice().getAmount())
                            .multiply(asset.effectiveMultiplier());

                    if (buyingPower.getAmount().compareTo(notional) < 0) {
                        throw InsufficientFundsException.of(
                                Money.of(notional, buyingPower.getCurrency()),
                                buyingPower
                        );
                    }
                }
            });
        }
    }

    private void publishOrderSubmittedEvent(Order order) {
        OrderSubmittedEvent event = OrderSubmittedEvent.builder()
                .orderId(order.getId())
                .ibOrderId(order.getIbOrderId())
                .accountId(order.getAccountId())
                .assetId(order.getAssetId())
                .symbol(order.getSymbol())
                .side(order.getSide().name())
                .orderType(order.getOrderType().name())
                .quantity(order.getQuantity())
                .limitPrice(order.getLimitPrice() != null ? order.getLimitPrice().getAmount() : null)
                .strategyRef(order.getStrategyRef())
                .build();
        domainEventPublisher.publish(event);
    }

    private void publishOrderCancelledEvent(Order order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .ibOrderId(order.getIbOrderId())
                .symbol(order.getSymbol())
                .reason(reason)
                .build();
        domainEventPublisher.publish(event);
    }
}
