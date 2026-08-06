package expression.builder.view;

import usace.hec.expressions.ExpressionNode;

import java.util.EventObject;

public class EditEvent extends EventObject {
    private int index;
    private String name;
    private ExpressionNode expression;
    private String expressionString;
    private Object defaultValue;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public EditEvent(Object source) {
        super(source);
    }

    /**
     * EventObject created to populate entries of {@link expression.builder.model.ExpressionEntry} when an edit event has occured (e.g. creating a new row, editing a new row);
     * @param source
     * @param name
     * @param expression
     * @param expressionString
     */
    public EditEvent(Object source, String name, ExpressionNode expression, String expressionString, Object defaultValue) {
        super(source);
        this.name = name;
        this.expression = expression;
        this.expressionString = expressionString;
        this.defaultValue = defaultValue;
    }
    public EditEvent(Object source,int index, String name, ExpressionNode expression, String expressionString, Object defaultValue) {
        super(source);
        this.index = index;
        this.name = name;
        this.expression = expression;
        this.expressionString = expressionString;
        this.defaultValue = defaultValue;
    }

    public int getIndex(){
        return index;
    }

    public String getName() {
        return name;
    }

    public ExpressionNode getExpression() {
        return expression;
    }

    public String getExpressionString() {
        return expressionString;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
}
