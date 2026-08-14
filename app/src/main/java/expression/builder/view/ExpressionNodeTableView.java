package expression.builder.view;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableRowSorter;

import expression.builder.model.ExpressionNodeTableModel;
import usace.hec.expressions.DisplayNode;

public class ExpressionNodeTableView extends JPanel {
    private final ExpressionNodeTableModel model;
    private final TableRowSorter<ExpressionNodeTableModel> sorter;

    public ExpressionNodeTableView(List<DisplayNode> nodes) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Node Table"));

        model = new ExpressionNodeTableModel(nodes);
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        //table.getColumnModel().getColumn(5).setPreferredWidth(150);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        add(new TableSearchBar("ExpressionNode Explorer", "Filter by name, operator, or category...", sorter), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
}