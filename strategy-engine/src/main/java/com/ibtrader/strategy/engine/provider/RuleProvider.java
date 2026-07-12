package com.ibtrader.strategy.engine.provider;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.port.outbound.ExpressionTreeRepository;
import com.ibtrader.domain.port.outbound.RuleRepository;
import com.ibtrader.domain.port.inbound.RuleEvaluationPort;
import lombok.RequiredArgsConstructor;
import com.ibtrader.domain.port.inbound.provider.DecisionProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleProvider implements DecisionProvider {

    private final RuleEvaluationPort ruleEvaluationEngine;
    private final ExpressionTreeRepository expressionTreeRepository;
    private final RuleRepository ruleRepository;

    @Override
    public List<TradeSignal> evaluate(EvaluationContext context) {
        return expressionTreeRepository.findByStrategyId(context.getStrategy().getId())
                .map(rootNode -> ruleEvaluationEngine.evaluate(
                        context, 
                        rootNode, 
                        ruleRepository.findByStrategyId(context.getStrategy().getId())))
                .orElseGet(ArrayList::new);
    }
}
