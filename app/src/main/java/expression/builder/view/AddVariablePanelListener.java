package expression.builder.view;

public interface AddVariablePanelListener {
    String importRequested();
    void discardImport();
    void hideScript(boolean hidden);
}
