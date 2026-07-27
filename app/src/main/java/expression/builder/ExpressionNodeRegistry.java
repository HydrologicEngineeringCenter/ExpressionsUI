package expression.builder;

import usace.hec.expressions.ExpressionNode;
import usace.hec.expressions.DisplayNode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Utility to discover all user-facing ExpressionNode implementations via reflection.
 *
 * <p>Only concrete classes implementing both {@link ExpressionNode} and
 * {@link DisplayNode} are discovered. Internal nodes (constants, variables,
 * parser intermediates) that do not implement DisplayNode are automatically
 * excluded from the UI.</p>
 *
 * <p>Metadata (display name, category, syntax template, arity) is obtained by
 * instantiating each node and querying its DisplayNode interface, replacing
 * the previous fragile string-based guessing.</p>
 */
public class ExpressionNodeRegistry {

    // -----------------------------------------------------------------
    // Descriptor
    // -----------------------------------------------------------------

    /**
     * Describes a discovered user-facing ExpressionNode with its UI metadata.
     *
     * <p>All metadata is sourced from the node's {@link DisplayNode} interface
     * rather than inferred from class names or hierarchy.</p>
     */
    public static class NodeDescriptor {
        private final Class<?> clazz;
        private final String simpleName;
        private final String displayName;
        private final String displayNameInfix;
        private final String category;
        private final String defaultSyntaxPrefix;
        private final String defaultSyntaxInfix;
        private final int arity;

        public NodeDescriptor(Class<?> clazz,
                              String displayName, String displayNameInfix,
                              String category,
                              String defaultSyntaxPrefix, String defaultSyntaxInfix,
                              int arity) {
            this.clazz = clazz;
            this.simpleName = clazz.getSimpleName();
            this.displayName = displayName;
            this.displayNameInfix = displayNameInfix;
            this.category = category;
            this.defaultSyntaxPrefix = defaultSyntaxPrefix;
            this.defaultSyntaxInfix = defaultSyntaxInfix;
            this.arity = arity;
        }

        public Class<?> getClazz() { return clazz; }
        public String getSimpleName() { return simpleName; }
        public String getDisplayName() { return displayName; }
        public String getDisplayNameInfix() { return displayNameInfix; }
        public String getCategory() { return category; }
        public String getDefaultSyntaxPrefix() { return defaultSyntaxPrefix; }
        public String getDefaultSyntaxInfix() { return defaultSyntaxInfix; }
        public int getArity() { return arity; }

        /** Return the syntax template for the requested format. */
        public String getDefaultSyntax(boolean infix) {
            return infix ? defaultSyntaxInfix : defaultSyntaxPrefix;
        }

        @Override
        public String toString() {
            return String.format("%s [%s] (arity: %d)", displayName, category, arity);
        }
    }

    // -----------------------------------------------------------------
    // Discovery
    // -----------------------------------------------------------------

    /**
     * Discover all user-facing ExpressionNode implementations on the classpath.
     * Scans the {@code usace.hec.expressions} package recursively.
     */
    public static List<NodeDescriptor> discoverAllNodes() {
        List<NodeDescriptor> descriptors = new ArrayList<>();
        descriptors.addAll(discoverInPackageRecursively("usace.hec.expressions"));
        return descriptors.stream()
                .sorted(Comparator.comparing(NodeDescriptor::getCategory)
                        .thenComparing(NodeDescriptor::getDisplayName))
                .collect(Collectors.toList());
    }

    private static List<NodeDescriptor> discoverInPackageRecursively(String packageName) {
        List<NodeDescriptor> results = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                if ("file".equals(protocol)) {
                    results.addAll(scanDirectoryRecursively(
                            new File(resource.getFile()), packageName));
                } else if ("jar".equals(protocol)) {
                    results.addAll(scanJarRecursively(resource, path, packageName));
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning package " + packageName + ": " + e.getMessage());
        }
        return results;
    }

    private static List<NodeDescriptor> scanDirectoryRecursively(File dir, String packageName) {
        List<NodeDescriptor> results = new ArrayList<>();
        if (!dir.isDirectory()) return results;
        File[] files = dir.listFiles();
        if (files == null) return results;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                results.addAll(analyzeClassQuietly(className));
            } else if (file.isDirectory()) {
                results.addAll(scanDirectoryRecursively(
                        file, packageName + "." + file.getName()));
            }
        }
        return results;
    }

    private static List<NodeDescriptor> scanJarRecursively(URL url,
            String basePackagePath, String basePackageName) throws IOException {
        List<NodeDescriptor> results = new ArrayList<>();
        JarURLConnection conn = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = conn.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                String entryName = entry.getName();
                if (entryName.startsWith(basePackagePath + "/")) {
                    String className = entryName.replace('/', '.');
                    className = className.substring(0, className.length() - 6);
                    results.addAll(analyzeClassQuietly(className));
                }
            }
        }
        return results;
    }

    // -----------------------------------------------------------------
    // Analysis -- DisplayNode-driven
    // -----------------------------------------------------------------

    private static List<NodeDescriptor> analyzeClassQuietly(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (ExpressionNode.class.isAssignableFrom(clazz)
                    && DisplayNode.class.isAssignableFrom(clazz)
                    && !clazz.isInterface()
                    && !Modifier.isAbstract(clazz.getModifiers())) {
                return analyzeClass(clazz);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Skip classes that can't be loaded
        }
        return new ArrayList<>();
    }

    private static List<NodeDescriptor> analyzeClass(Class<?> clazz) {
        List<NodeDescriptor> results = new ArrayList<>();
        try {
            DisplayNode instance = instantiateAsDisplayNode(clazz);
            if (instance != null) {
                String displayNamePrefix = instance.displayName(false);
                String displayNameInfix  = instance.displayName(true);
                String category          = instance.category();
                String defaultPrefix     = instance.defaultSyntax(false);
                String defaultInfix      = instance.defaultSyntax(true);
                int arity                = countCommas(defaultPrefix) + 1;

                results.add(new NodeDescriptor(clazz,
                        displayNamePrefix, displayNameInfix, category,
                        defaultPrefix, defaultInfix, arity));
            }
        } catch (Exception e) {
            System.err.println("Failed to instantiate " + clazz.getName()
                    + ": " + e.getMessage());
        }
        return results;
    }

    // -----------------------------------------------------------------
    // Instantiation helpers
    // -----------------------------------------------------------------

    /**
     * Instantiate a DisplayNode implementation with dummy child nodes.
     * Iterates over all declared constructors and attempts to resolve
     * arguments for each one.
     */
    @SuppressWarnings("unchecked")
    private static DisplayNode instantiateAsDisplayNode(Class<?> clazz) throws Exception {
        for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
            try {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] args = resolveArgs(paramTypes);
                if (args != null) {
                    ctor.setAccessible(true);
                    return (DisplayNode) ctor.newInstance(args);
                }
            } catch (Exception e) {
                // Try next constructor
            }
        }
        return null;
    }

    /**
     * Resolve constructor arguments by matching parameter types to dummy values.
     * Returns null if any parameter type cannot be resolved.
     */
    private static Object[] resolveArgs(Class<?>[] paramTypes) throws Exception {
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> pt = paramTypes[i];
            if (pt == boolean.class || pt == Boolean.class) {
                args[i] = false;
            } else if (pt == double.class || pt == Double.class) {
                args[i] = 0.0;
            } else if (pt == int.class || pt == Integer.class) {
                args[i] = 0;
            } else if (pt == String.class) {
                args[i] = "dummy";
            } else if (ExpressionNode.class.isAssignableFrom(pt)) {
                String name = pt.getSimpleName();
                if (name.contains("Boolean")) {
                    args[i] = createDummyLeaf(
                            "usace.hec.expressions.BooleanConstantNode",
                            Boolean.class, false);
                } else if (name.contains("Integer")) {
                    args[i] = createDummyLeaf(
                            "usace.hec.expressions.IntegerConstantNode",
                            int.class, 0);
                } else if (name.contains("String")) {
                    args[i] = createDummyLeaf(
                            "usace.hec.expressions.StringConstantNode",
                            String.class, "");
                } else {
                    // Default to Double (covers DoubleExpressionNode, ExpressionNode, etc.)
                    args[i] = createDummyLeaf(
                            "usace.hec.expressions.DoubleConstantNode",
                            double.class, 0.0);
                }
            } else {
                return null; // Unresolvable parameter type
            }
        }
        return args;
    }

    /**
     * Create a dummy leaf node by instantiating a constant node class.
     */
    private static ExpressionNode createDummyLeaf(String className,
            Class<?> paramType, Object value) throws Exception {
        Class<?> leafClass = Class.forName(className);
        Constructor<?> ctor = leafClass.getDeclaredConstructor(paramType);
        return (ExpressionNode) ctor.newInstance(value);
    }

    /**
     * Count comma characters in a string (for arity derivation via comma+1 rule).
     */
    private static int countCommas(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ',') count++;
        }
        return count;
    }
}