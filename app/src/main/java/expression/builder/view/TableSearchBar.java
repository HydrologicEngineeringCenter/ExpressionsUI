package expression.builder.view;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A labeled search field that live-filters {@code sorter}'s table, matching {@code promptText}
 * against every column of each row (case-insensitive). Shows {@code promptText} as greyed-out
 * placeholder text while empty and unfocused.
 */
public class TableSearchBar extends JPanel {

    public TableSearchBar(String labelText, String promptText, TableRowSorter<? extends TableModel> sorter) {
        setLayout(new BorderLayout(10, 5));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

        JTextField searchField = new JTextField(20);
        setupPrompt(searchField, promptText);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }

            private void applyFilter() {
                String text = searchField.getText();
                if (text.isEmpty() || text.equals(promptText)) {
                    sorter.setRowFilter(null);
                } else {
                    String lower = text.toLowerCase();
                    sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                        @Override
                        public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                            TableModel model = entry.getModel();
                            int row = entry.getIdentifier();
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

        add(new JLabel(labelText), BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);
    }

    private void setupPrompt(JTextField field, String prompt) {
        field.setText(prompt);
        field.setForeground(Color.GRAY);
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(prompt)) { field.setText(""); field.setForeground(Color.BLACK); }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) { field.setText(prompt); field.setForeground(Color.GRAY); }
            }
        });
    }
}