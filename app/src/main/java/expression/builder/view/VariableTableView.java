package expression.builder.view;

import expression.builder.model.ExpressionEntry;
import expression.builder.model.VariableTableModel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class VariableTableView extends JPanel {
    private JTable table;
    private final VariableTableModel model;
    private TableRowSorter<VariableTableModel> sorter;
    private JPopupMenu popup;
    private VariableTableListener listener;
    private boolean dataSet = false;

    public VariableTableView(){
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Variable Table"));
        setMinimumSize(new Dimension(400, 200));
        model = new VariableTableModel();
        table = new JTable(model);
        popup = new JPopupMenu();

        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);

        JMenuItem removeItem = new JMenuItem("Remove row");
        popup.add(removeItem);

        //popup appears when right clicking
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());

                table.getSelectionModel().setSelectionInterval(row, row);

                if(e.getButton() == MouseEvent.BUTTON3){
                    popup.show(table, e.getX(),e.getY());
                }
            }
        });

        //removes row from table
        removeItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();

                if (listener!=null){
                    listener.rowDeleted(row);
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
            int row = table.rowAtPoint(e.getPoint());

            table.getSelectionModel().setSelectionInterval(row, row);

            if(e.getButton() == MouseEvent.BUTTON1 && listener!=null){
                listener.getTextComment(row);
                if (e.getClickCount() == 2) {
                    listener.getExpressionText(row);
                }
            }
            }
        });

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
    public void refresh(){
        model.fireTableDataChanged();
    }

    public VariableTableModel getModel(){
        return this.model;
    }

    public void setData(List<ExpressionEntry> data) {
        model.setData(data);
        createTableRowSorter();
        dataSet = true;
    }
    public void createTableRowSorter() {
        if (!dataSet) {
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);


            JTextField searchField = new JTextField(20);
            setupPrompt(searchField, "Find a variable...");

            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    applyFilter();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    applyFilter();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    applyFilter();
                }

                private void applyFilter() {
                    String text = searchField.getText();
                    if (text.isEmpty() || text.equals("Find a variable...")) {
                        sorter.setRowFilter(null);
                    } else {
                        String lower = text.toLowerCase();
                        sorter.setRowFilter(new RowFilter<VariableTableModel, Integer>() {
                            @Override
                            public boolean include(Entry entry) {
                                int row = (int) entry.getIdentifier();
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
        }
    }
}
