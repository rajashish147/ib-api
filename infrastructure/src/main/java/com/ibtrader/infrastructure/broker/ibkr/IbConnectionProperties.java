package com.ibtrader.infrastructure.broker.ibkr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration properties for the Interactive Brokers TWS/Gateway connection.
 * Bound from the {@code app.ib} prefix in application.yml.
 *
 * <p>Example configuration:
 * <pre>{@code
 * app:
 *   ib:
 *     host: 127.0.0.1
 *     port: 4002          # 4002 = paper gateway, 4001 = live gateway
 *     client-id: 1
 *     paper-trading: true  # Safety flag — must be explicitly false for live
 * }</pre>
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app.ib")
public class IbConnectionProperties {

    /** Enables the IB adapter and its startup connection attempt. */
    private boolean enabled = false;

    /** TWS/Gateway host address. Default: localhost */
    @NotBlank
    private String host = "127.0.0.1";

    /**
     * TWS/Gateway port.
     * <ul>
     *   <li>4002 = IB Gateway paper trading</li>
     *   <li>4001 = IB Gateway live trading</li>
     *   <li>7497 = TWS paper trading</li>
     *   <li>7496 = TWS live trading</li>
     * </ul>
     */
    @Min(1024)
    @Max(65535)
    private int port = 4002;

    /**
     * IB API client ID. Must be unique per connected application.
     * If multiple instances connect, they must use different client IDs.
     */
    @Min(0)
    @Max(999)
    private int clientId = 1;

    /**
     * Safety flag. When {@code true}, prevents real order submission even if
     * connected to a live account port. Application checks this on startup.
     *
     * <p><b>Setting this to {@code false} enables real money trading.</b>
     * Requires explicit opt-in via environment variable:
     * {@code IB_PAPER_TRADING=false}.
     */
    private boolean paperTrading = true;

    /** IB account ID (e.g., "DU1234567" for paper, "U1234567" for live). */
    private String accountId = "";

    /** Connection management sub-properties */
    @Valid
    @NotNull
    private ConnectionProperties connection = new ConnectionProperties();

    /** Order submission sub-properties */
    @Valid
    @NotNull
    private OrderProperties orders = new OrderProperties();

    /**
     * Connection management configuration.
     */
    @Data
    public static class ConnectionProperties {

        /** Seconds between reconnect attempts after a disconnect. */
        @Min(5)
        @Max(300)
        private long reconnectIntervalSeconds = 30;

        /** Seconds between heartbeat checks (reqCurrentTime calls). */
        @Min(10)
        @Max(300)
        private long heartbeatIntervalSeconds = 60;

        /** Maximum number of reconnect attempts before giving up. */
        @Min(1)
        @Max(100)
        private int maxReconnectAttempts = 20;

        /** Seconds to wait for nextValidId callback during initial handshake. */
        @Min(5)
        @Max(60)
        private long connectionTimeoutSeconds = 15;
    }

    /**
     * Order submission configuration.
     */
    @Data
    public static class OrderProperties {

        /**
         * Use IB's Adaptive algo (improves fill quality at the cost of slower execution).
         * Set to {@code false} for immediate market orders.
         */
        private boolean useAdaptiveAlgo = false;

        /**
         * Default Time-In-Force for orders.
         * Common values: DAY, GTC, IOC, GTD.
         */
        @NotBlank
        private String tif = "DAY";
    }
}
