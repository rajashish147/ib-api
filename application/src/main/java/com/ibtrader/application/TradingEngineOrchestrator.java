package com.ibtrader.application;

import com.ibtrader.application.pipeline.PipelineContext;
import com.ibtrader.application.pipeline.PipelineStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates the central trading pipeline by executing all required stages
 * in order to evaluate strategies, manage risk, and plan orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradingEngineOrchestrator {

    private final List<PipelineStage> pipelineStages;

    /**
     * Executes the central trading pipeline for a given account.
     *
     * @param accountId the account ID to evaluate
     */
    public void executePipeline(String accountId) {
        log.info("Starting Trading Engine Pipeline for account: {}", accountId);

        PipelineContext context = new PipelineContext(accountId);

        List<PipelineStage> sortedStages = pipelineStages.stream()
                .sorted(Comparator.comparingInt(PipelineStage::getOrder))
                .toList();

        for (PipelineStage stage : sortedStages) {
            try {
                log.debug("Executing pipeline stage: {}", stage.getClass().getSimpleName());
                stage.execute(context);
            } catch (Exception e) {
                log.error("Pipeline stage {} failed for account {}: {}", 
                        stage.getClass().getSimpleName(), accountId, e.getMessage(), e);
                // Depending on the stage, we might want to halt the pipeline entirely.
                throw new IllegalStateException("Pipeline execution failed at stage " + stage.getClass().getSimpleName(), e);
            }
        }

        log.info("Pipeline execution completed for account: {}", accountId);
    }
}
