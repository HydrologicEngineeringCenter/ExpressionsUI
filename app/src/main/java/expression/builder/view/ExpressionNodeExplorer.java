package expression.builder.view;

import expression.builder.controller.ExpressionController;
import expression.builder.model.ExpressionNodeRegistry;
import usace.hec.expressions.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import expression.builder.model.ExpressionEntry;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.List;

public class ExpressionNodeExplorer {
    private ExpressionNode currentExpression;
    private ExpressionNodeTextBox textBox;
    private JLabel evaluationLabel;
    private JTextArea scriptTextArea;
    private JTextArea commentTextArea;
    private VariableTableView variableView;
    private ExpressionController expressionController;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { e.printStackTrace(); }
            new ExpressionNodeExplorer().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        List<DisplayNode> nodes = ExpressionNodeRegistry.getAllNodes();

        //Create whole frame
        JFrame frame = new JFrame("HEC Expression Builder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);

        //Creates left hand tab Pane
        JTabbedPane tabbedPane = new JTabbedPane();

        ExpressionNodeTableView tableView = new ExpressionNodeTableView(nodes);

        tabbedPane.add("Node Table", tableView);
        tabbedPane.setMinimumSize(new Dimension(150, 100));

        variableView = new VariableTableView();
        variableView.setMinimumSize(new Dimension(150, 100));

        tabbedPane.add("Variable Table", variableView);

        expressionController = new ExpressionController();

        variableView.setData(expressionController.getExpressions());

        commentTextArea = createCommentText();

        ExpressionNodeTreeView treeView = new ExpressionNodeTreeView(nodes, (descriptor, clicks) -> handleNodeInsertion(descriptor, clicks));

        treeView.setMinimumSize(new Dimension(150, 100));

        tabbedPane.add("Operations", treeView);

        //Creates right hand side typing textBox and evaluationLabel

        textBox = new ExpressionNodeTextBox();
        textBox.setTextUpdateListener(this::handleTextUpdate);
        evaluationLabel = createEvaluationLabel();
        scriptTextArea = createScriptText();


        JPanel expressionPanel = new JPanel(new BorderLayout());
        //Splitpane to edit the size of the box where typing happens and the box where the script format of the expression is shown.
        expressionPanel.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, textBox, new JScrollPane(scriptTextArea)), BorderLayout.CENTER);
        expressionPanel.add(evaluationLabel, BorderLayout.SOUTH);
        expressionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));




        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton saveButton =  new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    showAddVariableDialog();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        buttonPanel.add(saveButton);

        variableView.setVariableTableListener(new VariableTableListener() {
            @Override
            public void getExpression(int row) {
                //textBox.setNodeText(e.getExpressionNode());
                ExpressionEntry e = expressionController.getExpressions().get(row);
//                ExpressionOperator op = e.getExpressionNode().Operator();
//                ExpressionType t = e.getExpressionNode().resultType();
//                DisplayNode dn = new DisplayNode() {
//                    @Override
//                    public String displayName(boolean infix) {
//                        return infix ? op.getInfixName() : op.getPrefixName();
//                    }
//                    @Override
//                    public String category() {
//                        return op.getCategory();
//                    }
//                    @Override
//                    public String defaultSyntax(boolean infix) {
//                        return infix ? op.getInfixSyntax() : op.getPrefixSyntax();
//                    }
//                    @Override
//                    public List<ExpressionType> getExpressionResultTypes() {
//                        List<ExpressionType> result = new ArrayList<>();
//                        result.add(t);
//                        return result;
//                    }
//                    @Override
//                    public ExpressionOperator getOperator() {
//                        return op;
//                    }
//                };
                textBox.insertVariableAtCursor(e);
                //nameField.setText(e.getName());
            }

            @Override
            public void rowDeleted(int row) {
                expressionController.removeExpression(row);
                variableView.refresh();
            }
        });

        frame.setLayout(new BorderLayout());

        //Combines all lefthand components into a single panel on the left;
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(tabbedPane, BorderLayout.CENTER);
        leftPanel.add(commentTextArea, BorderLayout.SOUTH);
        leftPanel.setMinimumSize(new Dimension(150, 100));

        //Combines all righthand components into a single panel on the right;
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(expressionPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        //Combine the lefthand tabbedPane with tables and comments on all functions with the right panel where expression building takes place
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void showAddVariableDialog() throws Exception {
        JTextField nameField = new JTextField(20);
        JTextField defaultValueField = new JTextField(20);
        JComboBox<String> typeComboBox = new JComboBox<>();
        DefaultComboBoxModel<String> typeModel = new DefaultComboBoxModel<>();
        typeModel.addElement("Constant");
        typeModel.addElement("Updatable Variable");
        typeModel.addElement("Expression Holder");
        //TODO: Raise a text error if there are two Final Outputs
        typeModel.addElement("Final Output");
        typeComboBox.setModel(typeModel);
        typeComboBox.setSelectedIndex(0);
        defaultValueField.setEnabled(false);
        typeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                defaultValueField.setEnabled(typeComboBox.getSelectedItem().equals("Updatable Variable"));
            }
        });

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

        gc.gridx = 0;
        gc.gridy = 1;
        gc.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Default Value: "), gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(defaultValueField, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.anchor = GridBagConstraints.LINE_END;
        panel.add(new JLabel("Variable Type?"), gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        panel.add(typeComboBox, gc);

        //Dialog Pane creation
        JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = pane.createDialog("Add Variable");
        dialog.setResizable(true);
        dialog.setVisible(true);

        Object selected = pane.getValue();
        //check if OK was selected, return CLOSED_OPTION if any other action was taken to close the dialog.
        int result = (selected instanceof Integer) ? (Integer) selected : JOptionPane.CLOSED_OPTION;

        if (result != JOptionPane.OK_OPTION || nameField.getText().isEmpty()) {
            return;
        }

        String name = nameField.getText();
        String expression = textBox.getExpression();
        ExpressionNode defaultValueEvaluate = defaultValueField.getText().isEmpty() ? new DoubleConstantNode(0.0) : expressionController.parseExpression(defaultValueField.getText());
        Object defaultValue;
        String varType = (String) typeComboBox.getSelectedItem();
        assert varType != null;

        ExpressionNode newExp;
        if (!expression.isEmpty() && !varType.equals("Updatable Variable")) {
            try {
                newExp = expressionController.parseExpression(expression);
                defaultValue = "Unused";
            } catch (Exception ignored) {
                return;
            }
        } else if (varType.equals("Updatable Variable")) {
            switch (defaultValueEvaluate.resultType()){
                case ExpressionType.DOUBLE -> newExp = new DoubleVariableNode(name);
                case ExpressionType.BOOLEAN -> newExp = new BooleanVariableNode(name);
                case ExpressionType.INTEGER -> newExp = new IntegerVariableNode(name);
                case ExpressionType.STRING -> newExp = new StringVariableNode(name);
                case ExpressionType.DATE -> newExp = new DateTimeVariableNode(name);
                default -> throw new RuntimeException("Invalid Default Value");
            }
            expression = "[" + name + "]";
            defaultValue = expressionController.evaluateSafely(defaultValueEvaluate);
        } else {
            return;
        }

        EditEvent ev = new EditEvent(this, name, newExp, expression, varType, defaultValue);
        expressionController.putExpression(ev);
        variableView.refresh();
    }


    private JLabel createEvaluationLabel() {
        JLabel label = new JLabel("Evaluation: N/A");
        label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setForeground(new Color(0x2C, 0x5F, 0x8A));
        label.setBackground(new Color(0xF5, 0xF5, 0xF5));
        label.setOpaque(true);
        return label;
    }

    private JTextArea createScriptText(){
        JTextArea scriptText = new JTextArea();
        scriptText.setEditable(false);
        scriptText.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scriptText.setBackground(new Color(245, 245, 245));
        scriptText.setLineWrap(true);
        scriptText.setWrapStyleWord(true);
        scriptText.setBorder(new EmptyBorder(8, 8, 8, 8));
        scriptText.setText("");
        scriptText.setBorder(BorderFactory.createTitledBorder("Script Format"));
        return scriptText;
    }

    private JTextArea createCommentText(){
        JTextArea commentText = new JTextArea();
        commentText.setEditable(true);
        commentText.setFont(new Font("Monospaced", Font.PLAIN, 14));
        commentText.setLineWrap(true);
        commentText.setWrapStyleWord(true);
        commentText.setBorder(new EmptyBorder(8, 8, 8, 8));
        commentText.setText("");
        commentText.setBorder(BorderFactory.createTitledBorder("Description"));
        return commentText;
    }

    private void handleTextUpdate(String text) {
        try {
            if (!text.isEmpty()) {
                currentExpression = expressionController.parseExpression(text);
            } else {
                currentExpression = null;
            }
            updateEvaluationLabel();
            updateScriptTextArea(text);
        } catch (Exception e) {
            handleError(e, "Parse Error");
        }
    }

    private void handleNodeInsertion(DisplayNode descriptor, int clicks) {
        try {
            commentTextArea.setText(descriptor.getDescription());
            if (clicks == 2) {
                textBox.insertNodeAtCursor(descriptor);
            }
        } catch (Exception e) {
            // Fallback: insert the default syntax string directly.
            // This keeps the UI resilient even when constructor reflection fails.
            try {
                String fallbackSyntax = descriptor.defaultSyntax(false);
                if (fallbackSyntax != null && !fallbackSyntax.isEmpty()) {
                    textBox.insertTextAtCursor(fallbackSyntax);
                    return;
                }
            } catch (Exception ex) { /* ignore */ }

            // If fallback also fails, show error in the label
            handleError(e, "Insert Error");
        }
    }

    private void updateEvaluationLabel() {
        if (currentExpression == null) {
            evaluationLabel.setText("Evaluation: N/A");
            evaluationLabel.setForeground(new Color(0x2C, 0x5F, 0x8A));
            return;
        }
        try {
            Object result = expressionController.evaluateSafely(currentExpression);
            evaluationLabel.setText("Evaluation: " + (result != null ? result : "null"));
            evaluationLabel.setForeground(new Color(0x4C, 0xAF, 0x50));
        } catch (Exception e) {
            handleError(e, "Evaluation Error");
        }
    }
    private void updateScriptTextArea(String text) {
        if (currentExpression == null) {
            scriptTextArea.setText("N/A");
        }
        else {
            StringBuilder sb = new StringBuilder();
            int depth = 0;
            for (char c : text.toCharArray()) {
                if (c == '(') {
                    sb.append(c);
                    depth++;
                    sb.append('\n').append("\t".repeat(depth));
                } else if (c == ')') {
                    depth--;
                    sb.append(c);
                    sb.append('\n').append("\t".repeat(Math.max(0, depth)));
                } else if (c == ',') {
                    sb.append(c);
                    sb.append('\n').append("\t".repeat(Math.max(0, depth)));
                } else {
                    sb.append(c);
                }
            }
            scriptTextArea.setText(sb.toString());
        }
    }




    private void handleError(Exception e, String context) {
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (e.getCause() != null) {
            Throwable cause = e.getCause();
            msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        }
        evaluationLabel.setText("Evaluation: " + context + " (" + msg + ")");
        evaluationLabel.setForeground(new Color(0xD3, 0x2F, 0x2F));
        currentExpression = null;
    }
}