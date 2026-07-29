package expression.builder;

import org.apache.commons.math3.analysis.function.Exp;
import usace.hec.expressions.ExpressionNode;
import usace.hec.expressions.ExpressionParser;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class VariableTableView extends JPanel {
    private final VariableTableModel model;
    private final TableRowSorter<VariableTableModel> sorter;
    private VariableTableListener listener;

    public VariableTableView(List<ExpressionEntry> variables){
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Variable Table"));
        model = new VariableTableModel(variables);
        JTable table = new JTable(model);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField searchField = new JTextField(20);
        setupPrompt(searchField, "Find a variable...");

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());

                table.getSelectionModel().setSelectionInterval(row, row);

                if(e.getButton() == MouseEvent.BUTTON1 && listener!=null){
                    listener.getExpression(model.getData().get(row));
                }
            }
        });


        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }

            private void applyFilter() {
                String text = searchField.getText();
                if (text.isEmpty() || text.equals("Find a variable...")) {
                    sorter.setRowFilter(null);
                } else {
                    String lower = text.toLowerCase();
                    sorter.setRowFilter(new RowFilter<VariableTableModel, Integer>() {
                        @Override
                        public boolean include(Entry entry) {
                            int row = (int)entry.getIdentifier();
                            for (int i = 0; i < model.getColumnCount(); i++) {
                                if (String.valueOf(model.getValueAt(row, i)).toLowerCase().contains(lower)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                }
            }
        });

        JPanel controls = new JPanel(new BorderLayout(10, 5));
        controls.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        controls.add(new JLabel("ExpressionNode Explorer"), BorderLayout.WEST);
        controls.add(searchField, BorderLayout.CENTER);

        add(controls, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void setupPrompt(JTextField field, String prompt) {
        field.setText(prompt);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(prompt)) { field.setText(""); field.setForeground(Color.BLACK); }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) { field.setText(prompt); field.setForeground(Color.GRAY); }
            }
        });
    }


    public void setVariableTableListener(VariableTableListener listener){
        this.listener = listener;
    }
    public void refresh(){model.fireTableDataChanged();}

    public VariableTableModel getModel(){
        return this.model;
    }

    public void addExpression(ExpressionEntry newExp) {
        model.addData(newExp);
        refresh();
    }

    public int expressionExists(ExpressionEntry newExp) {
        List<ExpressionEntry> data = model.getData();
        String name = newExp.getName();
        for (int i = 0; i < data.size(); i++){
            if (name.equals(data.get(i).getName())){
                return i;
            }
        }
        return -1;
    }

    public void editEntry(int index, ExpressionEntry entry) {
        model.getData().set(index, entry);
        refresh();
    }


    public static class VariableTableModel extends AbstractTableModel{
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

    public static class ExpressionEntry implements Serializable {

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

        public void setName(String name) {
            this.name = name;
        }

        public ExpressionNode getExpressionNode() {
            return expressionNode;
        }

        public void setExpressionNode(ExpressionNode expressionNode) {
            this.expressionNode = expressionNode;
        }

        public String getExpression() {
            return expression;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }
    }
}
