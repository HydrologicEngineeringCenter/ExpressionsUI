package expression.builder.model;

import usace.hec.expressions.DisplayNode;
import usace.hec.expressions.DisplayNodeRegistry;
import usace.hec.expressions.DisplayNodeProvider;
import usace.hec.expressions.ExpressionOperator;
import usace.hec.expressions.ExpressionType;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * UI-facing registry that delegates to the core {@link DisplayNodeRegistry}.
 * Replaces legacy reflection scanning and {@code NodeDescriptor} model.
 */
public class ExpressionNodeRegistry {

    private static final DisplayNodeProvider coreRegistry = new DisplayNodeRegistry();

    /**
     * Returns all discovered expression operators as {@link DisplayNode} objects,
     * sorted by category and then prefix display name.
     */
    public static List<DisplayNode> getAllNodes() {
        return coreRegistry.getDisplayNodes().stream().filter(displayNode -> !displayNode.category().equals("LEAF"))
                .sorted(Comparator.comparing((DisplayNode displayNode) -> displayNode.category())
                        .thenComparing(n -> n.displayName(false)))
                .collect(Collectors.toList());
    }

    /**
     * Returns nodes filtered by category.
     */
    public static List<DisplayNode> getNodesByCategory(String category) {
        return coreRegistry.getDisplayNodesByCategory(category);
    }

    /**
     * Returns nodes that can produce the specified result type.
     */
    public static List<DisplayNode> getNodesByOutputType(ExpressionType type) {
        return coreRegistry.getDisplayNodesByOutputType(type);
    }

    /**
     * Returns all unique ExpressionOperators discovered in the system.
     */
    public static Set<ExpressionOperator> getAllOperators() {
        return getAllNodes().stream()
                .map(DisplayNode::getOperator)
                .collect(Collectors.toSet());
    }
}