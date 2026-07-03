package com.ibtrader.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradingEngineScheduler {

    private final TradingEngineOrchestrator tradingEngineOrchestrator;
    private final EngineState engineState;

    @Scheduled(fixedDelayString = "${ibtrader.engine.interval:60000}")
    public void runPipeline() {
        if (!engineState.isRunning()) {
            log.trace("Trading Engine is paused. Skipping execution.");
            return;
        }

        log.info("Starting scheduled Trading Engine Pipeline execution...");
        try {
            // Hardcoded account for now, this would come from configuration or database in a multi-tenant system
            tradingEngineOrchestrator.executePipeline("DUP854695");
            log.info("Scheduled Trading Engine Pipeline execution completed successfully.");
        } catch (Exception e) {
            log.error("Error executing scheduled Trading Engine Pipeline: {}", e.getMessage(), e);
        }
    }
}
