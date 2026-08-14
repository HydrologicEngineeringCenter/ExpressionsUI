package expression.builder.view;

public interface VariableTableListener {
    public void getExpressionText(int row);
    public void rowDeleted(int row);
    void getTextComment(int row);
    void editRequested(int row);
    void rowMoved(int fromRow, int toRow);
}
