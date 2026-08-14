package expression.builder.view;

import expression.builder.model.ExpressionEntry;
import expression.builder.model.VariableTableModel;

import javax.swing.*;
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
        JMenuItem editItem = new JMenuItem("Edit row");
        popup.add(removeItem);
        popup.add(editItem);

        //popup appears when right clicking
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());

                table.getSelectionModel().setSelectionInterval(row, row);

                if(e.getButton() == MouseEvent.BUTTON3){
                    popup.show(table, e.getX(),e.getY());
                }

                if (listener != null) listener.getTextComment(row);
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

        //helps to edit a row from table
        editItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();

                if (listener!=null){
                    listener.editRequested(row);
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
            add(new TableSearchBar("ExpressionNode Explorer", "Filter by name, operator, or category...", sorter), BorderLayout.NORTH);
        }
    }
}
