package com.ibtrader.application.pipeline;

/**
 * Represents a single stage in the trading engine pipeline.
 */
public interface PipelineStage {
    
    /**
     * Executes the stage logic using the provided context.
     *
     * @param context the pipeline context
     */
    void execute(PipelineContext context);
    
    /**
     * @return the order of this stage in the pipeline (lower is earlier)
     */
    int getOrder();
}
