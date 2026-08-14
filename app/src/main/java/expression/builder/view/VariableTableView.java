package expression.builder.view;

import expression.builder.model.ExpressionEntry;
import expression.builder.model.VariableTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class VariableTableView extends JPanel {
    private static final Color INVALID_ROW_COLOR = new Color(255, 205, 205);

    private JTable table;
    private final VariableTableModel model;
    private TableRowSorter<VariableTableModel> sorter;
    private JPopupMenu popup;
    private VariableTableListener listener;
    private boolean dataSet = false;
    private Set<Integer> invalidRows = Set.of();

    public VariableTableView(){
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Variable Table"));
        setMinimumSize(new Dimension(400, 200));
        model = new VariableTableModel();
        table = new JTable(model);
        popup = new JPopupMenu();

        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new RowReorderHandler());
        table.setDefaultRenderer(Object.class, new InvalidRowRenderer());

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

        //first click and hold allows for row dragging
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!filterActive() && table.getSelectedRow() >= 0) {
                    table.getTransferHandler().exportAsDrag(table, e, TransferHandler.MOVE);
                }
            }
        });

        //popup appears when right clicking
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());

                if(e.getButton() == MouseEvent.BUTTON3){
                    popup.show(table, e.getX(),e.getY());
                    table.getSelectionModel().setSelectionInterval(row, row);
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

    //Model-row indices whose expression references a variable not defined by an earlier row.
    public void setInvalidRows(Set<Integer> invalidRows) {
        this.invalidRows = invalidRows;
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

    private boolean filterActive() {
        return sorter != null && sorter.getRowFilter() != null;
    }

    private class InvalidRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                int modelRow = table.convertRowIndexToModel(row);
                c.setBackground(invalidRows.contains(modelRow) ? INVALID_ROW_COLOR : table.getBackground());
            }
            return c;
        }
    }

    //ROW DRAGGING LOGIC BELOW, NOT NECESSARY TO UNDERSTAND (Swing library takes care of things)


    /**
     * Row reordering is only enabled while the search bar's filter is inactive — filtered means the
     * view's row order no longer matches the model's, so a drop location wouldn't mean what it looks like.
     */
    private class RowReorderHandler extends TransferHandler {
        private static final DataFlavor ROW_INDEX_FLAVOR = new DataFlavor(Integer.class, "Row index");

        @Override
        public int getSourceActions(JComponent c) {
            return filterActive() ? TransferHandler.NONE : TransferHandler.MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            int selectedRow = table.getSelectedRow();
            return selectedRow < 0 ? null : new RowIndexTransferable(selectedRow);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return !filterActive() && support.isDrop() && support.isDataFlavorSupported(ROW_INDEX_FLAVOR);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                int fromRow = (int) support.getTransferable().getTransferData(ROW_INDEX_FLAVOR);
                int toRow = Math.min(((JTable.DropLocation) support.getDropLocation()).getRow(), table.getRowCount());
                if (listener != null) listener.rowMoved(fromRow, toRow);
                return true;
            } catch (UnsupportedFlavorException | IOException e) {
                return false;
            }
        }
    }

    private static class RowIndexTransferable implements Transferable {
        private final Integer rowIndex;

        RowIndexTransferable(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{RowReorderHandler.ROW_INDEX_FLAVOR};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return RowReorderHandler.ROW_INDEX_FLAVOR.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return rowIndex;
        }
    }
}
