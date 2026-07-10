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
    private volatile PositionHandler positionHandler = (acc, sym, cur, qty, cost) -> { };
    private volatile int nextValidOrderId;
    
    @FunctionalInterface
    public interface ErrorHandler {
        void onError(int id, int errorCode, String errorMsg, String advancedOrderRejectJson);
    }

    @FunctionalInterface
    public interface PositionHandler {
        void onPosition(String account, String symbol, String currency, double quantity, double avgCost);
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

    void setPositionHandler(PositionHandler positionHandler) {
        this.positionHandler = positionHandler;
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
            case "position" -> {
                // void position(String account, Contract contract, Decimal pos, double avgCost)
                if (args != null && args.length >= 4) {
                    try {
                        String account = (String) args[0];
                        Object contract = args[1];
                        Object posDecimal = args[2];
                        double avgCost = args[3] instanceof Double d ? d : ((Number) args[3]).doubleValue();
                        String symbol = readContractField(contract, "symbol", "m_symbol");
                        String currency = readContractField(contract, "currency", "m_currency");
                        double qty = Double.parseDouble(posDecimal.toString());
                        positionHandler.onPosition(account, symbol, currency, qty, avgCost);
                    } catch (Exception e) {
                        log.debug("Failed to parse position callback: {}", e.getMessage());
                    }
                }
            }
            default -> {
                // Other callbacks are intentionally ignored until their adapters exist.
            }
        }
        return defaultValue(method.getReturnType());
    }

    /** Try fieldName first, then legacy fieldName as fallback, using both field access and getter method. */
    private static String readContractField(Object contract, String modern, String legacy) throws Exception {
        for (String name : new String[]{modern, legacy}) {
            try {
                java.lang.reflect.Field f = contract.getClass().getField(name);
                Object val = f.get(contract);
                if (val instanceof String s) return s;
            } catch (NoSuchFieldException ignored) { /* try next */ }
            try {
                java.lang.reflect.Method m = contract.getClass().getMethod(name);
                Object val = m.invoke(contract);
                if (val instanceof String s) return s;
            } catch (NoSuchMethodException ignored) { /* try next */ }
        }
        return "";
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
