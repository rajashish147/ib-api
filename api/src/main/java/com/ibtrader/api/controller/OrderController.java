package com.ibtrader.api.controller;

import com.ibtrader.domain.model.common.Money;
import com.ibtrader.domain.model.order.Order;
import com.ibtrader.domain.model.order.OrderSide;
import com.ibtrader.domain.model.order.OrderType;
import com.ibtrader.domain.port.inbound.CancelOrderUseCase;
import com.ibtrader.domain.port.inbound.SubmitOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for manually submitting buy/sell orders directly to IBKR.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Manually submit buy or sell orders directly to Interactive Brokers")
public class OrderController {

    @Value("${app.ib.accounts.default.paper:DUP854695}")
    private String defaultAccountId;

    private final SubmitOrderUseCase submitOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    public record OrderRequest(
            @NotBlank String symbol,
            @NotNull UUID assetId,
            @NotNull OrderSide side,
            @NotNull OrderType orderType,
            @NotNull @DecimalMin("0.0001") BigDecimal quantity,
            BigDecimal limitPrice,
            String accountId
    ) {}

    public record OrderResponse(
            UUID orderId,
            String symbol,
            String side,
            String orderType,
            BigDecimal quantity,
            BigDecimal limitPrice,
            String status,
            String accountId
    ) {
        static OrderResponse from(Order o) {
            return new OrderResponse(
                    o.getId(),
                    o.getSymbol(),
                    o.getSide().name(),
                    o.getOrderType().name(),
                    o.getQuantity(),
                    o.getLimitPrice() != null ? o.getLimitPrice().getAmount() : null,
                    o.getStatus().name(),
                    o.getAccountId()
            );
        }
    }

    @Operation(summary = "Submit a buy or sell order",
            description = "Places a MARKET or LIMIT order directly to IBKR via the command outbox")
    @PostMapping
    public ResponseEntity<OrderResponse> submitOrder(@Valid @RequestBody OrderRequest req) {
        String accountId = req.accountId() != null ? req.accountId() : defaultAccountId;
        Money limitPrice = req.limitPrice() != null
                ? Money.of(req.limitPrice(), "USD")
                : null;

        SubmitOrderUseCase.Command command = new SubmitOrderUseCase.Command(
                accountId,
                req.assetId(),
                req.orderType(),
                req.side(),
                req.quantity(),
                limitPrice,
                null,
                "manual"
        );

        log.info("Manual order: {} {} {} qty={}", req.side(), req.orderType(), req.symbol(), req.quantity());
        Order order = submitOrderUseCase.execute(command);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @Operation(summary = "Cancel an order", description = "Cancels a pending or submitted order by its domain UUID")
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, String>> cancelOrder(@PathVariable UUID orderId) {
        log.info("Cancelling order: {}", orderId);
        cancelOrderUseCase.execute(new CancelOrderUseCase.Command(orderId, "manual"));
        return ResponseEntity.ok(Map.of("status", "cancelled", "orderId", orderId.toString()));
    }
}
