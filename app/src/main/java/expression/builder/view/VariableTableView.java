package expression.builder.view;

import expression.builder.model.ExpressionEntry;
import expression.builder.model.VariableTableModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    public int expressionExists(String name) {
        List<ExpressionEntry> data = model.getData();
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
}
