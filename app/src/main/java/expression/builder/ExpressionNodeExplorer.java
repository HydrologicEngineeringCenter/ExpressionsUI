package expression.builder;

import usace.hec.expressions.*;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

public class ExpressionNodeExplorer {
    private ExpressionNode currentExpression;
    private ExpressionNodeTextBox textBox;
    private JLabel evaluationLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { e.printStackTrace(); }
            new ExpressionNodeExplorer().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        List<ExpressionNodeRegistry.NodeDescriptor> nodes = ExpressionNodeRegistry.discoverAllNodes();

        JFrame frame = new JFrame("HEC Expression Builder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);

        textBox = new ExpressionNodeTextBox();
        textBox.setTextUpdateListener(this::handleTextUpdate);

        ExpressionNodeTableView tableView = new ExpressionNodeTableView(nodes);
        evaluationLabel = createEvaluationLabel();

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(evaluationLabel, BorderLayout.NORTH);
        bottomPanel.add(textBox, BorderLayout.CENTER);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 8));

        ExpressionNodeTreeView treeView = new ExpressionNodeTreeView(nodes, this::handleNodeInsertion);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableView, treeView);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.5);

        frame.setLayout(new BorderLayout());
        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
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
            currentExpression = parseExpression(text);
            updateEvaluationLabel();
        } catch (Exception e) {
            handleError(e, "Parse Error");
        }
    }

    private void handleNodeInsertion(ExpressionNodeRegistry.NodeDescriptor descriptor) {
        try {
            ExpressionNode newNode = instantiateDescriptor(descriptor);
            textBox.insertNodeAtCursor(newNode);
        } catch (Exception e) {
            // Fallback: insert the default syntax string directly.
            // This keeps the UI resilient even when constructor reflection fails.
            try {
                String fallbackSyntax = descriptor.getDefaultSyntax(false);
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
        if (result.isSuccess()) {
            return (ExpressionNode)result.getNode();
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

    private ExpressionNode instantiateDescriptor(ExpressionNodeRegistry.NodeDescriptor descriptor) throws Exception {
        Class<?> clazz = descriptor.getClazz();
        int arity = descriptor.getArity();

        ExpressionNode dummyDouble = createDummyLeaf(Double.class);
        ExpressionNode dummyBool = createDummyLeaf(Boolean.class);

        return switch (arity) {
            case 0 -> {
                try { yield (ExpressionNode) clazz.getConstructor().newInstance(); }
                catch (NoSuchMethodException e) { yield (ExpressionNode) clazz.getConstructor(String.class).newInstance("dummy"); }
            }
            case 1 -> {
                Constructor<?> ctor = clazz.getDeclaredConstructor(ExpressionNode.class);
                yield (ExpressionNode) ctor.newInstance(dummyDouble);
            }
            case 2 -> {
                Constructor<?> ctor = clazz.getDeclaredConstructor(ExpressionNode.class, ExpressionNode.class);
                yield (ExpressionNode) ctor.newInstance(dummyDouble, dummyDouble);
            }
            case 3 -> {
                Constructor<?> ctor = clazz.getDeclaredConstructor(
                        BooleanExpressionNode.class, ExpressionNode.class, ExpressionNode.class);
                yield (ExpressionNode) ctor.newInstance((BooleanExpressionNode) dummyBool, dummyDouble, dummyDouble);
            }
            default -> throw new IllegalStateException("Unsupported arity: " + arity);
        };
    }

    @SuppressWarnings("unchecked")
    private ExpressionNode createDummyLeaf(Class<?> type) throws Exception {
        Class<?> leafClass;
        Object value;
        if (type == Boolean.class || type == boolean.class) {
            leafClass = Class.forName("usace.hec.expressions.BooleanConstantNode");
            value = false;
        } else if (type == String.class) {
            leafClass = Class.forName("usace.hec.expressions.StringConstantNode");
            value = "";
        } else {
            leafClass = Class.forName("usace.hec.expressions.DoubleConstantNode");
            value = 0.0;
        }
        Constructor<?> ctor = leafClass.getDeclaredConstructor(type.isPrimitive() ? type : type.asSubclass(Object.class));
        return (ExpressionNode) ctor.newInstance(value);
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