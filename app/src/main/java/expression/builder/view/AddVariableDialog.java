package expression.builder.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

public final class AddVariableDialog {

    public record Result(String name, String comment, String variableType, String defaultValue) {
    }

    /**
     * Shows the Add/Edit Variable dialog. Pass {@code prefill} to pre-populate it for an edit,
     * or {@code null} for adding a new variable.
     * @return the submitted field values, or empty if the dialog was cancelled or no name was entered.
     */
    public static Optional<Result> show(Result prefill) {
        JTextField nameField = new JTextField(20);
        JTextField defaultValueField = new JTextField(20);
        JTextArea commentField = new JTextArea(1, 40);
        commentField.setLineWrap(true);
        JLabel defaultValueLabel = new JLabel("Default Value: ");
        JComboBox<String> typeComboBox = new JComboBox<>();

        //Initialize comboBox (dropdown component along with some listeners)
        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<>();
        typeModel.addElement("Constant");
        typeModel.addElement("Updatable Variable");
        typeModel.addElement("Expression Holder");
        //TODO: Raise a text error if there are two Final Outputs
        typeModel.addElement("Final Output");
        typeComboBox.setModel(typeModel);
        typeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                defaultValueField.setVisible(typeComboBox.getSelectedItem().equals("Updatable Variable"));
                defaultValueLabel.setVisible(typeComboBox.getSelectedItem().equals("Updatable Variable"));
            }
        });

        //Initialize Dialog Box if an edit is requested
        if (prefill != null) {
            nameField.setText(prefill.name());
            defaultValueField.setText(prefill.defaultValue());
            commentField.setText(prefill.comment());
            for (int i = 0; i < typeComboBox.getItemCount(); i++) {
                if (typeComboBox.getItemAt(i).equals(prefill.variableType())) {
                    typeComboBox.setSelectedIndex(i);
                    if (i == 1) {
                        defaultValueField.setVisible(true);
                        defaultValueLabel.setVisible(true);
                    }
                }
            }
        } else {
            nameField.setText("");
            defaultValueField.setText("");
            commentField.setText("");
            typeComboBox.setSelectedIndex(0);
            defaultValueField.setVisible(false);
            defaultValueLabel.setVisible(false);
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);

        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Name: "), gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(nameField, gc);

        //Next Row
        gc.gridx = 0;
        gc.gridy = 1;
        gc.anchor = GridBagConstraints.LINE_END;
        panel.add(defaultValueLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(defaultValueField, gc);

        //Next Row
        gc.gridx = 0;
        gc.gridy = 2;
        gc.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Comments: "), gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(commentField, gc);

        //Next Row
        gc.gridx = 0;
        gc.gridy = 3;
        gc.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Variable Type?"), gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(typeComboBox, gc);

        //Dialog Pane creation
        JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog("Add Variable");
        dialog.setResizable(true);
        //prevent textBoxes from shrinkng
        dialog.setMinimumSize(new Dimension(500, 200));
        dialog.setVisible(true);

        Object selected = pane.getValue();
        //check if OK was selected, return CLOSED_OPTION if any other action was taken to close the dialog.
        int result = (selected instanceof Integer) ? (Integer) selected : JOptionPane.CLOSED_OPTION;

        if (result != JOptionPane.OK_OPTION || nameField.getText().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new Result(nameField.getText(), commentField.getText(), (String) typeComboBox.getSelectedItem(), defaultValueField.getText()));
    }
}
