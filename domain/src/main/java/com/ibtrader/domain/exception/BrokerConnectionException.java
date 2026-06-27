package com.ibtrader.domain.exception;

/**
 * Broker-neutral failure reported when a trading gateway is unavailable.
 */
public final class BrokerConnectionException extends DomainException {

    public static final String ERROR_CODE = "BROKER_CONNECTION_FAILED";

    private final String broker;

    private BrokerConnectionException(String broker, String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
        this.broker = broker;
    }

    public static BrokerConnectionException unavailable(
            String broker,
            String message,
            Throwable cause) {

        if (broker == null || broker.isBlank()) {
            throw new IllegalArgumentException("broker must not be blank");
        }
        return new BrokerConnectionException(broker, message, cause);
    }

    public String getBroker() {
        return broker;
    }
}
