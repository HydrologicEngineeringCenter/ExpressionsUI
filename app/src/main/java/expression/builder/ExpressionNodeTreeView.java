package expression.builder;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

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
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (selectedNode == null || selectedNode.isRoot()) return;

            Object userObject = selectedNode.getUserObject();
            if (userObject instanceof ExpressionNodeRegistry.NodeDescriptor descriptor) {
                onNodeSelected.accept(descriptor);
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }
}