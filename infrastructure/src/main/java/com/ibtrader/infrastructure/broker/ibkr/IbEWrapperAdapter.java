package com.ibtrader.infrastructure.broker.ibkr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Reflection-backed callback adapter that avoids a compile-time dependency on
 * the license-gated IB API JAR.
 */
@Slf4j
@Component
public class IbEWrapperAdapter implements InvocationHandler {

    private volatile CountDownLatch handshakeLatch = new CountDownLatch(1);
    private volatile Consumer<String> disconnectHandler = ignored -> { };
    private volatile java.util.function.BiConsumer<Integer, Double> tickPriceHandler = (id, price) -> { };
    private volatile java.util.function.BiConsumer<Integer, String> orderStatusHandler = (id, status) -> { };
    private volatile ErrorHandler errorHandler = (id, code, msg, advanced) -> { };
    private volatile int nextValidOrderId;
    
    @FunctionalInterface
    public interface ErrorHandler {
        void onError(int id, int errorCode, String errorMsg, String advancedOrderRejectJson);
    }

    Object createProxy(Class<?> wrapperType) {
        return Proxy.newProxyInstance(
                wrapperType.getClassLoader(),
                new Class<?>[] {wrapperType},
                this);
    }

    void setDisconnectHandler(Consumer<String> disconnectHandler) {
        this.disconnectHandler = disconnectHandler;
    }

    void setTickPriceHandler(java.util.function.BiConsumer<Integer, Double> tickPriceHandler) {
        this.tickPriceHandler = tickPriceHandler;
    }

    void setOrderStatusHandler(java.util.function.BiConsumer<Integer, String> orderStatusHandler) {
        this.orderStatusHandler = orderStatusHandler;
    }

    void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    void resetHandshake() {
        handshakeLatch = new CountDownLatch(1);
    }

    boolean awaitNextValidId(long timeout, TimeUnit unit) {
        try {
            return handshakeLatch.await(timeout, unit);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    int getNextValidOrderId() {
        return nextValidOrderId;
    }

    synchronized int reserveNextOrderId() {
        return nextValidOrderId++;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "nextValidId" -> {
                nextValidOrderId = (Integer) args[0];
                handshakeLatch.countDown();
            }
            case "tickPrice" -> {
                // tickPrice(int tickerId, int field, double price, TickAttrib attribs)
                // fields: 1=bid, 2=ask, 4=last
                int field = (Integer) args[1];
                if (field == 1 || field == 2 || field == 4) {
                    tickPriceHandler.accept((Integer) args[0], (Double) args[2]);
                }
            }
            case "orderStatus" -> {
                // orderStatus(int orderId, String status, Decimal filled, Decimal remaining, 
                // double avgFillPrice, int permId, int parentId, double lastFillPrice, 
                // int clientId, String whyHeld, double mktCapPrice)
                orderStatusHandler.accept((Integer) args[0], (String) args[1]);
            }
            case "connectionClosed" -> disconnectHandler.accept("Broker closed the connection");
            case "error" -> {
                if (args != null && args.length >= 4) {
                    try {
                        Integer id = args[0] instanceof Integer ? (Integer) args[0] : -1;
                        Integer code = args[1] instanceof Integer ? (Integer) args[1] : -1;
                        String msg = args[2] != null ? args[2].toString() : "";
                        String advanced = args[3] != null ? args[3].toString() : "";
                        errorHandler.onError(id, code, msg, advanced);
                    } catch (Exception e) {
                        log.debug("Failed to parse error callback: {}", e.getMessage());
                    }
                } else if (args != null && args.length >= 3) {
                    try {
                        Integer id = args[0] instanceof Integer ? (Integer) args[0] : -1;
                        Integer code = args[1] instanceof Integer ? (Integer) args[1] : -1;
                        String msg = args[2] != null ? args[2].toString() : "";
                        errorHandler.onError(id, code, msg, "");
                    } catch (Exception e) {
                        log.debug("Failed to parse error callback: {}", e.getMessage());
                    }
                } else {
                    log.debug("IB callback error: {}", args == null ? "" : java.util.Arrays.toString(args));
                }
            }
            default -> {
                // Other callbacks are intentionally ignored until their adapters exist.
            }
        }
        return defaultValue(method.getReturnType());
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive() || returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }
}
