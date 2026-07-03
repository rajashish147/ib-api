package com.ibtrader.config;

import com.ibtrader.domain.engine.DefaultVariableRegistry;
import com.ibtrader.domain.engine.VariableRegistry;
import com.ibtrader.strategy.engine.DecisionEngine;
import com.ibtrader.strategy.engine.OrderPlanningEngine;
import com.ibtrader.strategy.engine.PortfolioAnalysisEngine;
import com.ibtrader.strategy.engine.RuleEvaluationEngine;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.MarketDataCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class to instantiate domain layer services.
 * The domain layer is pure Java and framework-agnostic, so we register
 * its components as Spring Beans here in the bootstrap module.
 */
@Configuration
public class DomainConfig {

    @Bean
    public VariableRegistry variableRegistry() {
        return new DefaultVariableRegistry();
    }

    @Bean
    public PortfolioAnalysisEngine portfolioAnalysisEngine(AssetRepository assetRepository, MarketDataCache marketDataCache) {
        return new PortfolioAnalysisEngine(assetRepository, marketDataCache);
    }

    @Bean
    public RuleEvaluationEngine ruleEvaluationEngine(VariableRegistry variableRegistry) {
        return new RuleEvaluationEngine(variableRegistry);
    }

    @Bean
    public DecisionEngine decisionEngine() {
        return new DecisionEngine();
    }

    @Bean
    public OrderPlanningEngine orderPlanningEngine(
            MarketDataCache marketDataCache, 
            AssetRepository assetRepository, 
            com.ibtrader.domain.port.outbound.ExecutionPolicyRepository executionPolicyRepository) {
        return new OrderPlanningEngine(marketDataCache, assetRepository, executionPolicyRepository);
    }
    
    @Bean
    public com.ibtrader.domain.engine.CooldownValidator cooldownValidator(
            com.ibtrader.domain.port.outbound.EvaluationHistoryRepository<
                    com.ibtrader.domain.model.strategy.EvaluationHistory> evaluationHistoryRepository) {
        return new com.ibtrader.domain.engine.CooldownValidator(evaluationHistoryRepository);
    }
}
