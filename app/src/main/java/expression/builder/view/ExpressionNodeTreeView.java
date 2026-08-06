package expression.builder.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.tree.*;

import usace.hec.expressions.DisplayNode;

public class ExpressionNodeTreeView extends JPanel {

    public ExpressionNodeTreeView(List<DisplayNode> nodes,
                                   Consumer<DisplayNode> onNodeSelected) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Operator Hierarchy"));

        // Group nodes by category
        Map<String, List<DisplayNode>> byCategory = new TreeMap<>();
        for (DisplayNode n : nodes) {
            byCategory.computeIfAbsent(n.category(), k -> new ArrayList<>()).add(n);
        }

        // Build tree: root -> category -> node descriptor
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("ExpressionOperators");
        for (Map.Entry<String, List<DisplayNode>> entry : byCategory.entrySet()) {
            DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(entry.getKey());
            for (DisplayNode n : entry.getValue()) {
                // Store the descriptor directly in the tree node
                catNode.add(new DefaultMutableTreeNode(n));
            }
            root.add(catNode);
        }

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        JTree tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.expandRow(0);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Custom renderer to show display name instead of toString()
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();

                if (userObject instanceof DisplayNode descriptor) {
                    setText(descriptor.getOperator().getPrefixName());
                    setToolTipText(String.format("Category: %s%nArity: %d%nSyntax: %s",
                            descriptor.category(),
                            descriptor.getOperator().getArity().getValue(),
                            descriptor.defaultSyntax(false)));
                } else {
                    setText(userObject.toString());
                    setToolTipText(null);
                }

                return this;
            }
        });

        // Selection handler: pull the descriptor directly from the tree node
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node == null || node.isRoot()) return;

                    Object userObject = node.getUserObject();
                    if (userObject instanceof DisplayNode descriptor) {
                        onNodeSelected.accept(descriptor);
                    }
                }
                //return;
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }
}