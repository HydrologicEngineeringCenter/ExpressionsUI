package expression.builder.view;

import expression.builder.controller.ExpressionController;
import expression.builder.model.ExpressionNodeRegistry;
import usace.hec.expressions.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import expression.builder.model.ExpressionEntry;
import expression.builder.util.ExpressionFormatter;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ExpressionNodeExplorer extends JPanel{
    private final static String STRING_EMPTY = "";
    private ExpressionNode currentExpression;
    private ExpressionNodeTextBox textBox;
    private JLabel evaluationLabel;
    private JTextArea scriptTextArea;
    private JTextArea commentTextArea;
    private JTextArea expresssionTextArea;
    private VariableTableView variableView;
    private ExpressionController expressionController;
    private JFrame frame;
    private JMenuBar menuBar;

    //Set when the editMenu popup requests an edit; carried through to prefill the next Add/Edit Variable dialog.
    private AddVariableDialog.Result pendingEdit;

    public ExpressionNodeExplorer() {
        initUI();
    }

    //Standalone launcher — demonstrates hosting this panel in its own top-level window. An embedding
    //app should instead construct ExpressionNodeExplorer directly and own the window lifecycle itself,
    //calling confirmSaveOnClose()/save() from its own close handling (this panel never calls System.exit).
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { e.printStackTrace(); }
            ExpressionNodeExplorer explorer = new ExpressionNodeExplorer();

            JFrame frame = new JFrame("HEC Expression Builder");
            //DO_NOTHING_ON_CLOSE so the close-confirmation dialog below can stop the close (Cancel); the
            //Yes/No branches drive the actual dispose/exit themselves once the save decision has been made.
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.setJMenuBar(explorer.getMenuBar());
            frame.setContentPane(explorer);
            frame.setSize(1100, 700);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (explorer.confirmSaveOnClose()) {
                        frame.dispose();
                        System.exit(0);
                    }
                }
            });
            frame.setVisible(true);
        });
    }

    private void initUI() {
        List<DisplayNode> nodes = ExpressionNodeRegistry.getAllNodes();

        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> save());
        fileMenu.add(saveItem);
        menuBar.add(fileMenu);

        //Creates left hand tab Pane
        JTabbedPane tabbedPane = new JTabbedPane();

        ExpressionNodeTableView tableView = new ExpressionNodeTableView(nodes);

        tabbedPane.add("Node Table", tableView);
        tabbedPane.setMinimumSize(new Dimension(150, 100));

        variableView = new VariableTableView();
        variableView.setMinimumSize(new Dimension(150, 100));

        tabbedPane.add("Variable Table", variableView);

        expressionController = new ExpressionController();

        try {
            expressionController.load();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load saved variables: " + ex.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }

        variableView.setData(expressionController.getExpressions());
        variableView.setInvalidRows(expressionController.revalidateRows());

        commentTextArea = createGenericText("Description");

        expresssionTextArea = createGenericText("Expanded Expression");

        expresssionTextArea.setVisible(false);
        commentTextArea.setVisible(false);


        ExpressionNodeTreeView treeView = new ExpressionNodeTreeView(nodes, (descriptor, clicks) -> handleNodeInsertion(descriptor, clicks));

        treeView.setMinimumSize(new Dimension(150, 100));

        tabbedPane.add("Operations", treeView);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (tabbedPane.getSelectedComponent().equals(variableView)) {
                    commentTextArea.setVisible(true);
                    commentTextArea.setBorder(BorderFactory.createTitledBorder("Comment"));
                    expresssionTextArea.setVisible(true);
                } else if (tabbedPane.getSelectedComponent().equals(treeView)) {
                    commentTextArea.setBorder(BorderFactory.createTitledBorder("Description"));
                    commentTextArea.setVisible(true);
                    expresssionTextArea.setVisible(false);
                } else {
                    commentTextArea.setVisible(false);
                    expresssionTextArea.setVisible(false);
                }
                commentTextArea.setText(STRING_EMPTY);
                expresssionTextArea.setText(STRING_EMPTY);
            }
        });

        //Creates right hand side typing textBox and evaluationLabel

        textBox = new ExpressionNodeTextBox();
        textBox.setTextUpdateListener(this::handleTextUpdate);
        evaluationLabel = createEvaluationLabel();
        scriptTextArea = createGenericText("Script Format");


        JPanel expressionPanel = new JPanel(new BorderLayout());
        //Splitpane to edit the size of the box where typing happens and the box where the script format of the expression is shown.
        expressionPanel.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, textBox, new JScrollPane(scriptTextArea)), BorderLayout.CENTER);
        expressionPanel.add(evaluationLabel, BorderLayout.SOUTH);
        expressionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));

        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton saveButton = new JButton("Save");
        JCheckBox syntaxType = new JCheckBox("Prefix Syntax?");

        //creates listener that creates an Add Variable Dialog
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

        syntaxType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentExpression != null) {
                    if (syntaxType.isSelected()) {
                        textBox.setInfixSyntax(false);
                    } else {
                        textBox.setInfixSyntax(true);
                    }
                    textBox.setExpressionNodeText(currentExpression);
                }
            }
        });

        buttonPanel.add(syntaxType);
        buttonPanel.add(saveButton);

        //sets a listener which allows ExpressionNodeExplorer to update if variableView is updated
        variableView.setVariableTableListener(new VariableTableListener() {
            @Override
            public void getExpressionText(int row) {
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
                refreshVariableView();
            }

            @Override
            public void editRequested(int row) {
                ExpressionEntry e = expressionController.getExpression(row);
                pendingEdit = new AddVariableDialog.Result(e.name(), e.comment(), e.variableType(), e.defaultValue().toString());
                if (!e.variableType().equals("Updatable Variable")) textBox.setExpressionNodeText(e.expressionNode());
            }

            @Override
            public void getTextComment(int row) {
                ExpressionEntry e = expressionController.getExpression(row);
                commentTextArea.setText(e.comment());
                expresssionTextArea.setText(e.expression());
            }

            @Override
            public void rowMoved(int fromRow, int toRow) {
                expressionController.moveExpression(fromRow, toRow);
                refreshVariableView();
                ;
            }
        });

        setLayout(new BorderLayout());

        //Combines all lefthand components into a single panel on the left;
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(tabbedPane, BorderLayout.CENTER);

        //Combine commentTextArea and Expression Text Area before putting into leftPanel
        JPanel leftText = new JPanel(new BorderLayout());
        leftText.add(commentTextArea, BorderLayout.NORTH);
        leftText.add(expresssionTextArea, BorderLayout.SOUTH);

        //Put combined text into leftPanel
        leftPanel.add(leftText, BorderLayout.SOUTH);
        leftPanel.setMinimumSize(new Dimension(150, 100));

        //Combines all righthand components into a single panel on the right;
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(expressionPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        //Combine the lefthand tabbedPane with tables and comments on all functions with the right panel where expression building takes place
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);

        add(splitPane, BorderLayout.CENTER);
    }
    /**
     * The File menu (currently just Save) built for this panel.
     * Note: As a JPanel, it has no window of its own to hang a menu bar off of, an embedding app can attach this to its own JFrame/JDialog via
     * setJMenuBar if it needs the shortcut, or ignore it and drive saving through save() directly using its own menu.
     */
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    /**
     * Persists the variable database to disk. Safe to call directly, an embedding app can trigger a save on its own close/dispose path.
     */
    public void save() {
        try {
            expressionController.save();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save variables: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Prompts to save before closing (Yes/No/Cancel) and saves if requested. This panel doesn't own a
     * window, so it can't veto a close by itself — host apps should call this from their own
     * close/dispose handling and only proceed with closing (dispose, System.exit, etc.) if it returns
     * {@code true}.
     */
    public boolean confirmSaveOnClose() {
        int choice = JOptionPane.showConfirmDialog(this, "Save changes before closing?", "Confirm Save",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            save();
        }
        return true;
    }

    private void showAddVariableDialog() throws Exception {
        Optional<AddVariableDialog.Result> formInput = AddVariableDialog.show(pendingEdit);
        pendingEdit = null;
        if (formInput.isEmpty()) {
            return;
        }

        AddVariableDialog.Result form = formInput.get();
        String expression = textBox.getExpression();

        EditEvent ev = expressionController.createEditEvent(this, form.name(), expression, form.comment(), form.variableType(), form.defaultValue());
        if (ev == null) {
            return;
        }

        expressionController.putExpression(ev);
        refreshVariableView();
    }

    //Recomputes which rows reference a variable not defined by an earlier row, then repaints the table.
    private void refreshVariableView() {
        variableView.setInvalidRows(expressionController.revalidateRows());
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

    private JTextArea createGenericText(String name) {
        JTextArea genericText = new JTextArea();
        genericText.setEditable(false);
        genericText.setFont(new Font("Monospaced", Font.PLAIN, 14));
        genericText.setBackground(new Color(245, 245, 245));
        genericText.setLineWrap(true);
        genericText.setWrapStyleWord(true);
        genericText.setBorder(new EmptyBorder(8, 8, 8, 8));
        genericText.setText(STRING_EMPTY);
        genericText.setBorder(BorderFactory.createTitledBorder(name));
        return genericText;
    }

    //called for every update in ExpressionPreview
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

    //Inserts nodes into ExpressionPreview when tree components are clicked
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

    //Called when ExpressionPreview is edited
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

    //Also called when ExpressionPreview is edited
    private void updateScriptTextArea(String text) {
        if (currentExpression == null) {
            scriptTextArea.setText("N/A");
        }
        else {
            scriptTextArea.setText(ExpressionFormatter.scriptPrint(text));
        }
    }



    //Error handling method to update evaluationLabel
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