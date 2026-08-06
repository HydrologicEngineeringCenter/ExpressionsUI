package expression.builder.view;

import expression.builder.controller.ExpressionController;
import expression.builder.model.ExpressionNodeRegistry;
import usace.hec.expressions.*;

import javax.swing.*;

import expression.builder.model.ExpressionEntry;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ExpressionNodeExplorer {
    private ExpressionNode currentExpression;
    private ExpressionNodeTextBox textBox;
    private JLabel evaluationLabel;
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

        JFrame frame = new JFrame("HEC Expression Builder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);

        textBox = new ExpressionNodeTextBox();
        textBox.setTextUpdateListener(this::handleTextUpdate);

        ExpressionNodeTableView tableView = new ExpressionNodeTableView(nodes);
        evaluationLabel = createEvaluationLabel();


        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.add("Node Table", tableView);

        variableView = new VariableTableView();


        tabbedPane.add("Variable Table", variableView);

        expressionController = new ExpressionController();

        variableView.setData(expressionController.getExpressions());

        JPanel expressionPanel = new JPanel(new BorderLayout());
        expressionPanel.add(textBox, BorderLayout.CENTER);
        expressionPanel.add(evaluationLabel, BorderLayout.SOUTH);
        expressionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));


        ExpressionNodeTreeView treeView = new ExpressionNodeTreeView(nodes, this::handleNodeInsertion);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, treeView);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        JTextField nameField = new JTextField(20);
        JTextField defaultValueField = new JTextField(20);
        JCheckBox updatableCheck = new JCheckBox();

        JButton saveButton =  new JButton("Save");

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

            @Override
            public void rowAddedorEditRequested(EditEvent ev) {
                expressionController.putExpression(ev);
                variableView.refresh();
            }
        });

        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!nameField.getText().isEmpty()){
                    ExpressionEntry newExp;
                    String name = nameField.getText();
                    String expression = textBox.getExpression();
                    String defaultValField = defaultValueField.getText();
                    double doubleVal = 0;
                    try {doubleVal = Double.parseDouble(defaultValueField.getText());
                    } catch (Exception ignored){
                        //Nothing happens if parseDouble is null
                    }
                    if (!expression.isEmpty() && !updatableCheck.isSelected()) {
                        try {
                            ExpressionNode expNode = parseExpression(expression);
                            newExp = new ExpressionEntry(name, expression, expNode,0.0);
                        } catch (Exception ignored) {
                            return;
                        }
                    } else {
                        ExpressionNode updatable = new UpdateableLeafNode(name);
                        newExp = new ExpressionEntry(name, "[" + name + "]", updatable, doubleVal);
                    }
                    variableView.saveExpression(newExp);
                    return;
                }
                return;
            }
        });

        updatableCheck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (updatableCheck.isSelected()) {
                    defaultValueField.setEnabled(true);
                } else {
                    defaultValueField.setEnabled(false);
                }
            }
        });

        updatableCheck.setSelected(false);
        defaultValueField.setEnabled(false);
        textBox.setEnabled(true);

        gc.weightx = 1;
        gc.weighty = 1;

        gc.gridy = 0;

        gc.gridx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = new Insets(0, 0, 0, 5);
        buttonPanel.add(new JLabel("Name: "), gc);

        gc.gridx = 1;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(nameField, gc);

        gc.gridx = 2;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = new Insets(0, 0, 0, 5);
        buttonPanel.add(new JLabel("Default Value: "), gc);

        gc.gridx = 3;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(defaultValueField, gc);

        gc.gridx = 4;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_END;
        gc.insets = new Insets(0, 0, 0, 5);
        buttonPanel.add(new JLabel("Updatable?"), gc);

        gc.gridx = 5;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(updatableCheck, gc);


        gc.gridx = 6;
        gc.anchor = GridBagConstraints.LAST_LINE_START;
        buttonPanel.add(saveButton, gc);

        frame.setLayout(new BorderLayout());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(expressionPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        JSplitPane nestedFrame = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane, bottomPanel);

        frame.add(nestedFrame, BorderLayout.CENTER);
        frame.setVisible(true);
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

    private void handleTextUpdate(String text) {
        try {
            if (!text.isEmpty()) {
                currentExpression = parseExpression(text);
            } else {
                currentExpression = null;
            }
            updateEvaluationLabel();
        } catch (Exception e) {
            handleError(e, "Parse Error");
        }
    }

    private void handleNodeInsertion(DisplayNode descriptor) {
        try {
            textBox.insertNodeAtCursor(descriptor);
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

    private ExpressionNode parseExpression(String text) throws Exception {
        ExpressionParser parser = new ExpressionParser();
        ParseResult result = parser.parse(text);
        List<ExpressionEntry> data = expressionController.getExpressions();
        DataHub dh = new DataHub();
        for(ExpressionEntry e:data){
            if(e.getExpressionNode() instanceof DataRequester){
                dh.setValue(((DataRequester)e.getExpressionNode()).getName(), e.getDefaultValue());
            }
        }
        if (result.isSuccess()) {
            ExpressionNode node = (ExpressionNode)result.getNode();
            node.setProvider(dh);
            return node;
        }
        throw new IllegalArgumentException(result.getError() + " at position " + result.getError().position());
    }

    private void updateEvaluationLabel() {
        if (currentExpression == null) {
            evaluationLabel.setText("Evaluation: N/A");
            evaluationLabel.setForeground(new Color(0x2C, 0x5F, 0x8A));
            return;
        }
        try {
            Object result = evaluateSafely(currentExpression);
            evaluationLabel.setText("Evaluation: " + (result != null ? result : "null"));
            evaluationLabel.setForeground(new Color(0x4C, 0xAF, 0x50));
        } catch (Exception e) {
            handleError(e, "Evaluation Error");
        }
    }

    private Object evaluateSafely(ExpressionNode node) throws Exception {
        Method evalMethod = node.getClass().getMethod("evaluate");
        return evalMethod.invoke(node);
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