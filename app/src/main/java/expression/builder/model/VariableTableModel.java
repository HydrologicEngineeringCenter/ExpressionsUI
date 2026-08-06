package expression.builder.model;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class VariableTableModel extends AbstractTableModel {
    private final List<ExpressionEntry> data;

    public VariableTableModel(List<ExpressionEntry> data){
        this.data = data;
    }

    private final String[] columns = {"Index", "Name", "Expression", "Default Value"};

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

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ExpressionEntry d = data.get(rowIndex);
        return switch(columnIndex) {
            case 0 -> rowIndex;
            case 1 -> d.getName();
            case 2 -> d.getExpression();
            case 3 -> d.getDefaultValue();
            default -> "";
        };
    }

    public List<ExpressionEntry> getData(){
        return this.data;
    }

    public void addData(ExpressionEntry newExp) {
        data.add(newExp);
    }
}