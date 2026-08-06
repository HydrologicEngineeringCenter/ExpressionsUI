package expression.builder.model;

import usace.hec.expressions.ExpressionNode;

import java.io.Serial;
import java.io.Serializable;

public class ExpressionEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 2L;

    private String name;
    private ExpressionNode expressionNode;
    private String expression;
    private Object defaultValue;

    /**
     * Creates an ExpressionEntry which contains a row index, a name to refer to at compute time, the {@link ExpressionNode} itself, and a comment the user would like to add.
     * @param name
     * @param expressionNode
     * @param expression
     */
    public ExpressionEntry(String name, String expression, ExpressionNode expressionNode, Object defaultValue){
        this.name = name;
        this.expression = expression;
        this.expressionNode = expressionNode;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public ExpressionNode getExpressionNode() {
        return expressionNode;
    }
    public String getExpression() {
        return expression;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

}
