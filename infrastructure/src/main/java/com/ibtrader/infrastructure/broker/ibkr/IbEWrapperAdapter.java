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
    private volatile int nextValidOrderId;

    Object createProxy(Class<?> wrapperType) {
        return Proxy.newProxyInstance(
                wrapperType.getClassLoader(),
                new Class<?>[] {wrapperType},
                this);
    }

    void setDisconnectHandler(Consumer<String> disconnectHandler) {
        this.disconnectHandler = disconnectHandler;
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

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "nextValidId" -> {
                nextValidOrderId = (Integer) args[0];
                handshakeLatch.countDown();
            }
            case "connectionClosed" -> disconnectHandler.accept("Broker closed the connection");
            case "error" -> log.debug("IB callback error: {}", args == null ? "" : java.util.Arrays.toString(args));
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
