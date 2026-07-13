package com.ibtrader.strategy.engine;

import com.ibtrader.domain.engine.EvaluationContext;
import com.ibtrader.domain.engine.VariableRegistry;
import com.ibtrader.domain.model.strategy.ExpressionNode;
import com.ibtrader.domain.model.strategy.RuleAction;
import com.ibtrader.domain.model.strategy.TradeSignal;
import com.ibtrader.domain.port.inbound.RuleEvaluationPort;
import lombok.RequiredArgsConstructor;
import java.util.logging.Logger;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Evaluates a strategy's expression tree against a given evaluation context.
 * Uses the VariableRegistry to dynamically resolve variables.
 */
@RequiredArgsConstructor
public class RuleEvaluationEngine implements RuleEvaluationPort {

    private static final Logger LOG = Logger.getLogger(RuleEvaluationEngine.class.getName());

    private final VariableRegistry variableRegistry;

    /**
     * Evaluates a strategy rule tree. If it resolves to TRUE, generates TradeSignals
     * based on the associated actions.
     *
     * @param context the context (portfolio, metrics, etc.)
     * @param rootNode the root of the expression tree
     * @param actions the actions to trigger if the rule evaluates to true
     * @return a list of generated TradeSignals
     */
    @Override
    public List<TradeSignal> evaluate(EvaluationContext context, ExpressionNode rootNode, List<RuleAction> actions) {
        List<TradeSignal> signals = new ArrayList<>();
        
        try {
            boolean isMatch = evaluateNode(rootNode, context);
            if (isMatch) {
                LOG.info(String.format("Rule matched for strategy: %s", context.getStrategy().getName()));
                
                for (RuleAction action : actions) {
                    TradeSignal signal = TradeSignal.builder()
                            .id(UUID.randomUUID())
                            .strategyId(context.getStrategy().getId())
                            .symbol(action.getSymbol())
                            .action(action.getActionType())
                            .quantityType(action.getQuantityType())
                            .quantityValue(action.getQuantityValue())
                            .reason("Rule evaluation passed")
                            .confidence(1.0)
                            .generatedAt(Instant.now())
                            .build();
                    signals.add(signal);
                }
            }
        } catch (Exception e) {
            LOG.severe(String.format("Error evaluating tree for strategy: %s. Error: %s", 
                context.getStrategy().getId(), e.getMessage()));
        }
        
        return signals;
    }

    private boolean evaluateNode(ExpressionNode node, EvaluationContext context) {
        if ("LOGICAL_AND".equalsIgnoreCase(node.getNodeType()) || "AND".equalsIgnoreCase(node.getNodeType())) {
            for (ExpressionNode child : node.getChildren()) {
                if (!evaluateNode(child, context)) {
                    return false;
                }
            }
            return true;
        } else if ("LOGICAL_OR".equalsIgnoreCase(node.getNodeType()) || "OR".equalsIgnoreCase(node.getNodeType())) {
            boolean result = false;
            for (ExpressionNode child : node.getChildren()) {
                if (evaluateNode(child, context)) {
                    result = true;
                    break;
                }
            }
            return result;
        } else if ("CONDITION".equalsIgnoreCase(node.getNodeType())) {
            return evaluateCondition(node, context);
        }
        
        throw new IllegalArgumentException("Unknown node type: " + node.getNodeType());
    }

    private boolean evaluateCondition(ExpressionNode node, EvaluationContext context) {
        // Resolve left operand
        BigDecimal leftValue = resolveOperand(node.getLeftOperand(), context);
        // Resolve right operand
        BigDecimal rightValue = resolveOperand(node.getRightOperand(), context);

        int comparison = leftValue.compareTo(rightValue);

        return switch (node.getOperator()) {
            case ">" -> comparison > 0;
            case ">=" -> comparison >= 0;
            case "<" -> comparison < 0;
            case "<=" -> comparison <= 0;
            case "==" -> comparison == 0;
            case "!=" -> comparison != 0;
            default -> throw new IllegalArgumentException("Unsupported operator: " + node.getOperator());
        };
    }

    private BigDecimal resolveOperand(String operand, EvaluationContext context) {
        // Try parsing as a literal number first
        try {
            return new BigDecimal(operand);
        } catch (NumberFormatException e) {
            // Not a literal, resolve via registry
            Optional<BigDecimal> resolved = variableRegistry.resolve(operand, context);
            return resolved.orElseThrow(() -> 
                new IllegalStateException("Failed to resolve variable: " + operand));
        }
    }
}
