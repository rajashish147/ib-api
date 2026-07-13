package com.ibtrader.infrastructure.broker.ibkr;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Small reflective facade over the optional IB API JAR.
 */
@Component
public class IbApiClient {

    private Object signal;
    private Object socket;
    private Object reader;

    public void connect(
            String host,
            int port,
            int clientId,
            IbEWrapperAdapter wrapperAdapter) {

        try {
            Class<?> wrapperType = Class.forName("com.ib.client.EWrapper");
            Class<?> signalType = Class.forName("com.ib.client.EReaderSignal");
            Class<?> javaSignalType = Class.forName("com.ib.client.EJavaSignal");
            Class<?> socketType = Class.forName("com.ib.client.EClientSocket");
            Class<?> readerType = Class.forName("com.ib.client.EReader");

            signal = javaSignalType.getConstructor().newInstance();
            Object wrapper = wrapperAdapter.createProxy(wrapperType);
            Constructor<?> socketConstructor =
                    socketType.getConstructor(wrapperType, signalType);
            socket = socketConstructor.newInstance(wrapper, signal);
            invoke(socket, "eConnect",
                    new Class<?>[] {String.class, int.class, int.class},
                    host, port, clientId);

            if (!isConnected()) {
                throw new IbConnectionException("IB client did not open a connection");
            }

            Constructor<?> readerConstructor =
                    readerType.getConstructor(socketType, signalType);
            reader = readerConstructor.newInstance(socket, signal);
            invoke(reader, "start", new Class<?>[0]);
        } catch (ClassNotFoundException exception) {
            throw new IbConnectionException(
                    "IB API JAR is unavailable. Add a licensed TwsApi*.jar to libs/ "
                            + "before enabling app.ib.enabled.",
                    exception);
        } catch (ReflectiveOperationException exception) {
            throw new IbConnectionException("Unable to initialize the IB API client", exception);
        }
    }

    public boolean isConnected() {
        if (socket == null) {
            return false;
        }
        return (Boolean) invoke(socket, "isConnected", new Class<?>[0]);
    }

    public void processMessages() {
        if (signal == null || reader == null) {
            return;
        }
        invoke(signal, "waitForSignal", new Class<?>[0]);
        invoke(reader, "processMsgs", new Class<?>[0]);
    }

    public void requestCurrentTime() {
        requireSocket();
        invoke(socket, "reqCurrentTime", new Class<?>[0]);
    }

    public void requestPositions() {
        requireSocket();
        invoke(socket, "reqPositions", new Class<?>[0]);
    }

    public void placeStockOrder(
            int orderId,
            String symbol,
            String side,
            BigDecimal quantity,
            BigDecimal limitPrice) {

        requireSocket();
        try {
            Class<?> contractType = Class.forName("com.ib.client.Contract");
            Class<?> orderType = Class.forName("com.ib.client.Order");
            Class<?> decimalType = Class.forName("com.ib.client.Decimal");

            Object contract = contractType.getConstructor().newInstance();
            invoke(contract, "symbol", new Class<?>[] {String.class}, symbol);
            invoke(contract, "secType", new Class<?>[] {String.class}, "STK");
            invoke(contract, "exchange", new Class<?>[] {String.class}, "SMART");
            invoke(contract, "currency", new Class<?>[] {String.class}, "USD");

            Object order = orderType.getConstructor().newInstance();
            invoke(order, "action", new Class<?>[] {String.class}, side);
            invoke(order, "orderType", new Class<?>[] {String.class}, limitPrice == null ? "MKT" : "LMT");
            Object ibQuantity = invokeStatic(
                    decimalType,
                    "get",
                    new Class<?>[] {String.class},
                    quantity.stripTrailingZeros().toPlainString());
            invoke(order, "totalQuantity", new Class<?>[] {decimalType}, ibQuantity);

            if (limitPrice != null) {
                invoke(order, "lmtPrice", new Class<?>[] {double.class}, limitPrice.doubleValue());
            }

            invoke(socket, "placeOrder", new Class<?>[] {int.class, contractType, orderType},
                    orderId, contract, order);
        } catch (ClassNotFoundException exception) {
            throw new IbConnectionException(
                    "IB API JAR is unavailable. Add a licensed TwsApi*.jar to libs/ "
                            + "before submitting orders.",
                    exception);
        } catch (ReflectiveOperationException exception) {
            throw new IbConnectionException("Unable to build IB order for " + symbol, exception);
        }
    }

    public String serverVersion() {
        requireSocket();
        Object version = invoke(socket, "serverVersion", new Class<?>[0]);
        return String.valueOf(version);
    }

    /**
     * Sets the market data type to request from IB.
     * <ul>
     *   <li>1 = Live (real-time)</li>
     *   <li>2 = Frozen (last real-time value)</li>
     *   <li>3 = Delayed (free 15-min delayed data — works without subscriptions)</li>
     *   <li>4 = Delayed frozen</li>
     * </ul>
     * Must be called before {@link #requestMarketData} to take effect.
     */
    public void requestMarketDataType(int marketDataType) {
        requireSocket();
        invoke(socket, "reqMarketDataType", new Class<?>[] {int.class}, marketDataType);
    }

    public void requestMarketData(int tickerId, String symbol, String exchange, String currency) {
        requireSocket();
        try {
            Class<?> contractType = Class.forName("com.ib.client.Contract");
            Object contract = contractType.getConstructor().newInstance();
            invoke(contract, "symbol", new Class<?>[] {String.class}, symbol);
            invoke(contract, "secType", new Class<?>[] {String.class}, "STK");
            invoke(contract, "exchange", new Class<?>[] {String.class}, exchange != null ? exchange : "SMART");
            invoke(contract, "currency", new Class<?>[] {String.class}, currency != null ? currency : "USD");

            invoke(socket, "reqMktData",
                    new Class<?>[] {
                        int.class, contractType, String.class,
                        boolean.class, boolean.class, java.util.List.class
                    },
                    tickerId, contract, "", false, false, null);
        } catch (ClassNotFoundException exception) {
            throw new IbConnectionException("IB API JAR is unavailable.", exception);
        } catch (ReflectiveOperationException exception) {
            throw new IbConnectionException("Unable to request market data for " + symbol, exception);
        }
    }


    public void disconnect() {
        if (socket != null) {
            invoke(socket, "eDisconnect", new Class<?>[0]);
        }
        reader = null;
        socket = null;
        signal = null;
    }

    private void requireSocket() {
        if (!isConnected()) {
            throw new IbConnectionException("IB client is not connected");
        }
    }

    private static Object invoke(
            Object target,
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments) {

        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, arguments);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IbConnectionException(
                    "IB API method is unavailable: " + methodName, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IbConnectionException("IB API call failed: " + methodName, cause);
        }
    }

    private static Object invokeStatic(
            Class<?> targetType,
            String methodName,
            Class<?>[] parameterTypes,
            Object... arguments) {

        try {
            Method method = targetType.getMethod(methodName, parameterTypes);
            return method.invoke(null, arguments);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IbConnectionException(
                    "IB API method is unavailable: " + methodName, exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IbConnectionException("IB API call failed: " + methodName, cause);
        }
    }
}
