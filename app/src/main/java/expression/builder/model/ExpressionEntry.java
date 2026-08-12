package expression.builder.model;

import usace.hec.expressions.ExpressionNode;

import java.io.Serial;
import java.io.Serializable;

public record ExpressionEntry(String name, String expression, ExpressionNode expressionNode, String variableType, Object defaultValue) implements Serializable {
    @Serial
    private static final long serialVersionUID = 2L;
}
