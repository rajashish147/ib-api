package com.ibtrader.scheduler;

import com.ibtrader.application.TradingEngineOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.logging.Logger;

/**
 * Trigger for the Trading Engine Orchestrator.
 * Keeps scheduling concerns completely separate from business logic.
 */
@Component
@RequiredArgsConstructor
public class TradingPipelineScheduler {

    private static final Logger LOG = Logger.getLogger(TradingPipelineScheduler.class.getName());

    private final TradingEngineOrchestrator orchestrator;

    @Value("${app.ib.account-id:DU123456}")
    private String defaultAccountId;

    @Value("${app.strategy.evaluation-interval-seconds:60}")
    private int evaluationIntervalSeconds;

    /**
     * Fixed interval execution based on configuration.
     * Starts evaluating 10 seconds after application startup to allow IBKR connection.
     */
    @Scheduled(initialDelay = 10000, fixedDelayString = "${app.strategy.evaluation-interval-seconds:60}000")
    public void executeFixedInterval() {
        LOG.info("Trading pipeline triggered.");
        try {
            orchestrator.executePipeline(defaultAccountId);
            LOG.info("Trading pipeline cycle completed.");
        } catch (Exception e) {
            LOG.severe(String.format("Market-open pipeline execution failed: %s", e.getMessage()));
        }
    }

    /**
     * Cron-based execution example (runs at 9:30 AM EST on weekdays).
     */
    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "America/New_York")
    public void executeAtMarketOpen() {
        LOG.info("Triggering market-open pipeline execution...");
        try {
            orchestrator.executePipeline(defaultAccountId);
        } catch (Exception e) {
            LOG.severe("Error in market-open pipeline cycle: " + e.getMessage());
        }
    }
}
