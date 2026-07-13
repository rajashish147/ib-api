package com.ibtrader.scheduler;

import com.ibtrader.application.EngineState;
import com.ibtrader.application.TradingEngineOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Trigger for the Trading Engine Orchestrator.
 * Keeps scheduling concerns completely separate from business logic.
 *
 * <p>This is the single scheduled entry point into {@link TradingEngineOrchestrator}.
 * A previous duplicate scheduler ({@code com.ibtrader.application.TradingEngineScheduler})
 * was removed because running two independent schedulers against the same pipeline caused
 * overlapping executions (risking duplicate order submissions) and made the pause/resume
 * API unreliable, since only one of the two schedulers consulted {@link EngineState}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingPipelineScheduler {

    private final TradingEngineOrchestrator orchestrator;
    private final EngineState engineState;

    @Value("${app.ib.account-id:DUP854695}")
    private String defaultAccountId;

    /**
     * Fixed interval execution based on configuration.
     * Starts evaluating 10 seconds after application startup to allow IBKR connection.
     */
    @Scheduled(initialDelay = 10000, fixedDelayString = "${app.strategy.evaluation-interval-seconds:60}000")
    public void executeFixedInterval() {
        if (!engineState.isRunning()) {
            log.trace("Trading Engine is paused. Skipping scheduled execution.");
            return;
        }

        log.info("Trading pipeline triggered.");
        try {
            orchestrator.executePipeline(defaultAccountId);
            log.info("Trading pipeline cycle completed.");
        } catch (Exception e) {
            log.error("Trading pipeline execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Cron-based execution example (runs at 9:30 AM EST on weekdays).
     */
    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "America/New_York")
    public void executeAtMarketOpen() {
        if (!engineState.isRunning()) {
            log.trace("Trading Engine is paused. Skipping market-open execution.");
            return;
        }

        log.info("Triggering market-open pipeline execution...");
        try {
            orchestrator.executePipeline(defaultAccountId);
        } catch (Exception e) {
            log.error("Error in market-open pipeline cycle: {}", e.getMessage(), e);
        }
    }
}
