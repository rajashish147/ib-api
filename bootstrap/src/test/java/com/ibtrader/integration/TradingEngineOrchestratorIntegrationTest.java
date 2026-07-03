package com.ibtrader.integration;

import com.ibtrader.application.TradingEngineOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class TradingEngineOrchestratorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TradingEngineOrchestrator orchestrator;

    @Test
    public void testExecutePipelineEmptyState() {
        // When there are no portfolios or active strategies, the pipeline should just complete quickly
        orchestrator.executePipeline("TEST_ACCOUNT_123");
        
        // Assert that the orchestrator is not null and pipeline executes without exception
        assertThat(orchestrator).isNotNull();
    }
}
