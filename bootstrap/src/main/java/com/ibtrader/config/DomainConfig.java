package com.ibtrader.config;

import com.ibtrader.domain.engine.DefaultVariableRegistry;
import com.ibtrader.domain.engine.VariableRegistry;
import com.ibtrader.domain.port.inbound.DecisionEnginePort;
import com.ibtrader.domain.port.inbound.OrderPlanningPort;
import com.ibtrader.domain.port.inbound.PortfolioAnalysisPort;
import com.ibtrader.domain.port.inbound.RuleEvaluationPort;
import com.ibtrader.domain.port.inbound.provider.DecisionProvider;
import com.ibtrader.domain.port.inbound.provider.DecisionProviderRegistry;
import com.ibtrader.domain.port.outbound.AssetRepository;
import com.ibtrader.domain.port.outbound.ExecutionPolicyRepository;
import com.ibtrader.domain.port.outbound.MarketDataCache;
import com.ibtrader.strategy.engine.DecisionEngine;
import com.ibtrader.strategy.engine.OrderPlanningEngine;
import com.ibtrader.strategy.engine.PortfolioAnalysisEngine;
import com.ibtrader.strategy.engine.RuleEvaluationEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring configuration class to instantiate domain layer services.
 * The domain layer is pure Java and framework-agnostic, so we register
 * its components as Spring Beans here in the bootstrap module.
 *
 * <p><strong>IMPORTANT</strong>: All {@code @Bean} methods return the <em>port interface</em>
 * type, not the concrete class. Spring registers beans by declared return type, so returning
 * {@code DecisionEngine} instead of {@code DecisionEnginePort} would cause any injection site
 * that declares {@code DecisionEnginePort} to fail with {@code NoSuchBeanDefinitionException}.
 */
@Configuration
public class DomainConfig {

    @Bean
    public VariableRegistry variableRegistry() {
        return new DefaultVariableRegistry();
    }

    @Bean
    public PortfolioAnalysisPort portfolioAnalysisEngine(
            AssetRepository assetRepository, MarketDataCache marketDataCache) {
        return new PortfolioAnalysisEngine(assetRepository, marketDataCache);
    }

    @Bean
    public RuleEvaluationPort ruleEvaluationEngine(VariableRegistry variableRegistry) {
        return new RuleEvaluationEngine(variableRegistry);
    }

    @Bean
    public DecisionEnginePort decisionEngine() {
        return new DecisionEngine();
    }

    @Bean
    public OrderPlanningPort orderPlanningEngine(
            MarketDataCache marketDataCache,
            AssetRepository assetRepository,
            ExecutionPolicyRepository executionPolicyRepository) {
        return new OrderPlanningEngine(marketDataCache, assetRepository, executionPolicyRepository);
    }

    /**
     * Collects all DecisionProvider @Component beans (PriceThresholdDecisionProvider,
     * RuleProvider, MachineLearningDecisionProvider, PortfolioGoalDecisionProvider,
     * TechnicalIndicatorDecisionProvider) into a single registry.
     * Spring automatically injects all beans implementing DecisionProvider as the List parameter.
     */
    @Bean
    public DecisionProviderRegistry decisionProviderRegistry(List<DecisionProvider> providers) {
        return new DecisionProviderRegistry(providers);
    }
}
