package expression.builder.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;

public final class AddVariablePanel extends JPanel {

    private ImportListener listener;

    public record Result(String name, String comment, String variableType, String defaultValue, String expression) {
    }

    private final JTextField nameField = new JTextField(40);
    private final JTextField defaultValueField = new JTextField(20);
    private final JTextArea commentField = new JTextArea();
    private final JLabel importLabel = new JLabel("Import Name: ");
    private final JTextArea importField = new JTextArea();
    private final JLabel defaultValueLabel = new JLabel("Default Value: ");
    private final JComboBox<String> typeComboBox = new JComboBox<>();
    private final JButton importBtn = new JButton("Import");

    /**
     * Shows the Add/Edit Variable Panel, which rests above the Expression Writing panel.
     */
    public AddVariablePanel() {
        importField.setEditable(false);
        importField.setBackground(new Color(245, 245, 245));
        commentField.setLineWrap(true);
        commentField.setWrapStyleWord(true);
        typeComboBox.setPreferredSize(new Dimension(150,20));

        //Initialize comboBox (dropdown component along with some listeners)
        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<>();
        //TODO: convert elements into Enums
        typeModel.addElement("Constant");
        typeModel.addElement("Updatable Variable");
        typeModel.addElement("Expression Holder");
        //TODO: Raise a text error if there are two Final Outputs
        typeModel.addElement("Final Output");
        typeComboBox.setModel(typeModel);

        typeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean selected = typeComboBox.getSelectedItem().equals("Updatable Variable");
                if (!selected){
                    if (listener !=null) {
                        listener.discardImport();
                        defaultValueField.setText("");
                        importField.setText("");
                    }
                }
                defaultValueField.setVisible(selected);
                defaultValueLabel.setVisible(selected);
                importField.setVisible(selected);
                importLabel.setVisible(selected);
                importBtn.setVisible(selected);
                revalidate();

            }
        });

        reset();

        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);

        gc.weightx = 2;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(new JLabel("Name: "), gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(nameField, gc);

        gc.gridx = 2;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(new JLabel("Variable Type: "), gc);

        gc.gridx = 3;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(typeComboBox, gc);

        //Next Row
        gc.gridx = 0;
        gc.gridy = 1;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(defaultValueLabel, gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        add(defaultValueField, gc);

        //Next Row
        gc.gridx = 0;
        gc.gridy = 3;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(importLabel, gc);

        gc.gridx = 1;
        gc.weighty = 1.0; //added weight to allow vertical expansion
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.BOTH; //fill added to allow expansion
        gc.anchor = GridBagConstraints.LINE_START;
        add(importField, gc);

        gc.gridx = 3;
        gc.gridwidth = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.NONE;
        add(importBtn, gc);


        //Next Row
        gc.gridx = 0;
        gc.gridy = 4;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        add(new JLabel("Comments: "), gc);

        gc.gridx = 1;
        gc.weighty = 1.0; //added weight to allow vertical expansion
        gc.gridwidth = 3;
        gc.fill = GridBagConstraints.BOTH; //fill added to allow expansion
        gc.anchor = GridBagConstraints.LINE_START;
        add(commentField, gc);

        //set Listener
        importBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String variable = listener.importRequested();
                //Treat a null/blank result (host had nothing to offer, or the user cancelled its picker)
                //as "no selection made" rather than clobbering whatever was previously imported.
                if (variable != null && !variable.isBlank()) {
                    importField.setText(variable);
                }
            }
        });


        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBorder(BorderFactory.createTitledBorder("Add Variable"));
    }

    /**
     * Populates the form for editing an existing variable, or clears it back to blank if
     * {@code prefill} is {@code null}.
     */
    public void setPrefill(Result prefill) {
        if (prefill == null) {
            reset();
            return;
        }
        nameField.setText(prefill.name());
        defaultValueField.setText(prefill.defaultValue());
        commentField.setText(prefill.comment());
        for (int i = 0; i < typeComboBox.getItemCount(); i++) {
            if (typeComboBox.getItemAt(i).equals(prefill.variableType())) {
                typeComboBox.setSelectedIndex(i);
                if (i == 1) {
                    defaultValueField.setVisible(true);
                    defaultValueLabel.setVisible(true);
                    importField.setVisible(true);
                    importLabel.setVisible(true);
                    importBtn.setVisible(true);
                    importField.setText(prefill.expression());
                }
            }
        }
    }

    /** Clears the form back to a blank "add new variable" state. */
    public void reset() {
        nameField.setText("");
        defaultValueField.setText("");
        commentField.setText("");
        importField.setText("");
        typeComboBox.setSelectedIndex(0);
        defaultValueField.setVisible(false);
        defaultValueLabel.setVisible(false);
        importField.setVisible(false);
        importLabel.setVisible(false);
        importBtn.setVisible(false);
    }

    /**
     * @return {@code true} if the form currently has enough input to submit:
     * Updatable Variable: a completed import (an empty import would persist a row with no link back to the host's variable).
     * Other: non-empty name
     */
    public boolean hasValidInput() {
        if (nameField.getText().isEmpty()) {
            return false;
        }
        if (typeComboBox.getSelectedItem().equals("Updatable Variable") && importField.getText().isEmpty()) {
            return false;
        }
        return true;
    }

    /** @return the current field values, regardless of {@link #hasValidInput()}. */
    public Result getResult() {
        return new Result(nameField.getText(), commentField.getText(), (String) typeComboBox.getSelectedItem(), defaultValueField.getText(), importField.getText());
    }

    public void setListener(ImportListener listener){
        this.listener = listener;
    }
}
