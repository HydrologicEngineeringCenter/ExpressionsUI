package expression.builder;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.tree.*;

public class ExpressionNodeTreeView extends JPanel {

    public ExpressionNodeTreeView(List<ExpressionNodeRegistry.NodeDescriptor> nodes,
                                   Consumer<ExpressionNodeRegistry.NodeDescriptor> onNodeSelected) {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Operator Hierarchy"));

        // Group nodes by category
        Map<String, List<ExpressionNodeRegistry.NodeDescriptor>> byCategory = new TreeMap<>();
        for (ExpressionNodeRegistry.NodeDescriptor n : nodes) {
            byCategory.computeIfAbsent(n.getCategory(), k -> new ArrayList<>()).add(n);
        }

        // Build tree: root -> category -> node descriptor
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("ExpressionOperators");
        for (Map.Entry<String, List<ExpressionNodeRegistry.NodeDescriptor>> entry : byCategory.entrySet()) {
            DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(entry.getKey());
            for (ExpressionNodeRegistry.NodeDescriptor n : entry.getValue()) {
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

                if (userObject instanceof ExpressionNodeRegistry.NodeDescriptor descriptor) {
                    setText(descriptor.getDisplayName());
                    setToolTipText(String.format("Category: %s%nArity: %d%nSyntax: %s",
                            descriptor.getCategory(),
                            descriptor.getArity(),
                            descriptor.getDefaultSyntax(false)));
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
                    if (userObject instanceof ExpressionNodeRegistry.NodeDescriptor descriptor) {
                        onNodeSelected.accept(descriptor);
                    }
                }
                return;
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }
}