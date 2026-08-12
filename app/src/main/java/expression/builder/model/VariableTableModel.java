package expression.builder.model;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class VariableTableModel extends AbstractTableModel {
    private List<ExpressionEntry> data;

    public VariableTableModel(){};

    public VariableTableModel(List<ExpressionEntry> data){
        this.data = data;
    }

    private final String[] columns = {"Index", "Type", "Name", "Expression", "Default Value", "Comment"};

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    public void setData(List<ExpressionEntry> data){
        this.data = data;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ExpressionEntry d = data.get(rowIndex);
        return switch(columnIndex) {
            case 0 -> rowIndex;
            case 1 -> d.variableType();
            case 2 -> d.name();
            case 3 -> d.variableType().equals("Expression Holder") || d.variableType().equals("Final Output") ? "Click to View" : d.expression();
            case 4 -> d.defaultValue();
            case 5 -> "Click to View";
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return super.isCellEditable(rowIndex, columnIndex);
    }
}