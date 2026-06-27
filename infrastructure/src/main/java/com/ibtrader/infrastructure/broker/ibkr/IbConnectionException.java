package com.ibtrader.infrastructure.broker.ibkr;

/**
 * IB adapter failure. Vendor-specific connection details remain outside the domain.
 */
public final class IbConnectionException extends RuntimeException {

    public IbConnectionException(String message) {
        super(message);
    }

    public IbConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
