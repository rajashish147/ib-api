package com.ibtrader.domain.model.strategy;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.UUID;

/**
 * Represents a node in the expression tree for rule evaluation.
 * A node can be a logical operator (AND, OR) or a leaf condition (>, <, ==).
 */
@Getter
@Builder
@ToString
public class ExpressionNode {
    private final UUID id;
    private final String nodeType; // AND, OR, CONDITION
    
    // For logical nodes
    private final List<ExpressionNode> children;
    
    // For condition nodes
    private final String leftOperand;
    private final String operator;
    private final String rightOperand;
}
