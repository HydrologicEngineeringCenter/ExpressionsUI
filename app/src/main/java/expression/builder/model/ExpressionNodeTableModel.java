package expression.builder.model;

import usace.hec.expressions.DisplayNode;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class ExpressionNodeTableModel extends AbstractTableModel {
    private final List<DisplayNode> data;
    private final String[] columns = {"Category", "Operator", "Infix", "Arity", "Syntax"};

    public ExpressionNodeTableModel(List<DisplayNode> data) { this.data = data; }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return columns.length; }
    @Override public String getColumnName(int col) { return columns[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        DisplayNode d = data.get(row);
        return switch (col) {
            case 0 -> d.category();
            case 1 -> d.displayName(false);
            case 2 ->  d.displayName(true);
            case 3 -> d.getOperator().getArity();
            case 4 -> d.defaultSyntax(false);
            default -> "";
        };
    }
}
