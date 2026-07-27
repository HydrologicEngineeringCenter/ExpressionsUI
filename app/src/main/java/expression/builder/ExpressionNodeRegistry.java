package expression.builder;

import usace.hec.expressions.ExpressionNode;
import usace.hec.expressions.DisplayNode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
 * <p>Nodes are discovered if they implement {@link ExpressionNode}. Metadata is sourced
 * from a {@code public static DisplayNode displayData()} method if present.</p>
 */
public class ExpressionNodeRegistry {

    public static class NodeDescriptor {
        private final Class<?> clazz;
        private final String simpleName;
        private final String displayName;
        private final String displayNameInfix;
        private final String category;
        private final String defaultSyntaxPrefix;
        private final String defaultSyntaxInfix;
        private final int arity;

        public NodeDescriptor(Class<?> clazz, String displayName, String displayNameInfix,
                              String category, String defaultSyntaxPrefix, String defaultSyntaxInfix, int arity) {
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
        
        public String getDefaultSyntax(boolean infix) {
            return infix ? defaultSyntaxInfix : defaultSyntaxPrefix;
        }

        @Override
        public String toString() {
            return String.format("%s [%s] (arity: %d)", displayName, category, arity);
        }
    }

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
                    results.addAll(scanDirectoryRecursively(new File(resource.getFile()), packageName));
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
                results.addAll(scanDirectoryRecursively(file, packageName + "." + file.getName()));
            }
        }
        return results;
    }

    private static List<NodeDescriptor> scanJarRecursively(URL url, String basePackagePath, String basePackageName) throws IOException {
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

    private static List<NodeDescriptor> analyzeClassQuietly(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            // Check if it's an ExpressionNode, not abstract, not interface
            // (Note: We no longer require DisplayNode to be implemented on the class itself)
            if (ExpressionNode.class.isAssignableFrom(clazz)
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
        
        // 1. Try to find a static displayData() method (Preferred)
        try {
            
            Method displayDataMethod = clazz.getMethod("displayData");
            // Check if return type is DisplayNode (or assignable)
            if (DisplayNode.class.isAssignableFrom(displayDataMethod.getReturnType())) {
                // Invoke static method (target is null)
                DisplayNode metadata = (DisplayNode) displayDataMethod.invoke(null); 
                results.add(createDescriptor(clazz, metadata));
                return results;
            }
        } catch (NoSuchMethodException e) {
            // No displayData method, fall through to fallback or instantiation if needed
            //System.err.println(clazz.getMethods());
        } catch (Exception e) {
            System.err.println("Error reading displayData for " + clazz + ": " + e.getMessage());
        }


        return results;
    }

    private static NodeDescriptor createDescriptor(Class<?> clazz, DisplayNode metadata) {
        String displayNamePrefix = metadata.displayName(false);
        String displayNameInfix  = metadata.displayName(true);
        String category          = metadata.category();
        String defaultPrefix     = metadata.defaultSyntax(false);
        String defaultInfix      = metadata.defaultSyntax(true);
        int arity                = countCommas(defaultPrefix) + 1;

        return new NodeDescriptor(clazz, displayNamePrefix, displayNameInfix, category,
                defaultPrefix, defaultInfix, arity);
    }


    private static int countCommas(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ',') count++;
        }
        return count;
    }
}