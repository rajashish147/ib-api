package com.ibtrader.application;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class EngineState {

    private final AtomicBoolean running = new AtomicBoolean(true);

    public boolean isRunning() {
        return running.get();
    }

    public void pause() {
        running.set(false);
    }

    public void resume() {
        running.set(true);
    }
}
