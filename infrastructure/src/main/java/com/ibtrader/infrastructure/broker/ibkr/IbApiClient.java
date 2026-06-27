package com.ibtrader.infrastructure.broker.ibkr;

import org.springframework.stereotype.Component;

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

    public String serverVersion() {
        requireSocket();
        Object version = invoke(socket, "serverVersion", new Class<?>[0]);
        return String.valueOf(version);
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
}
