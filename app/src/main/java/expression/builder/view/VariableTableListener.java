package expression.builder.view;

import expression.builder.model.ExpressionEntry;

public interface VariableTableListener {
    public void getExpression(int row);
    public void rowDeleted(int row);
    public void rowAddedorEditRequested(EditEvent ev);
}
