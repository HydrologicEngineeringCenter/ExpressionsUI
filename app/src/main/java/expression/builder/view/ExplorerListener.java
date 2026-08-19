package expression.builder.view;

public interface ExplorerListener {
    String importUpdatable();
    void confirmUpdatableSaved(String name);
    void discardImport();
    void replace(String oldName, String name);
}
