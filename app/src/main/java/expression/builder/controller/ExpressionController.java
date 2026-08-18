package expression.builder.controller;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import expression.builder.model.VariableDataBase;
import expression.builder.model.ExpressionEntry;
import expression.builder.view.EditEvent;
import usace.hec.expressions.*;
import expression.builder.util.DataHub;

/**
 *  @apiNote Internal wiring — not intended for use outside this application.
 * All panels access the database of {@link ExpressionEntry} through an instance of this class (can most likely be static).
 */
public class ExpressionController {
    private static final Path SAVE_FILE = Paths.get(System.getProperty("user.home"), ".hecExpressionBuilder", "variables.ser");

    VariableDataBase db = new VariableDataBase();
    DataHub dh = new DataHub();

    /**
     * {@code db.getExpressions()} returns a pointer to the list of db, but the panels can't modify it.
     *
     * @return
     */
    public List<ExpressionEntry> getExpressions() {
        return db.getExpressions();
    }

    public DataProvider getDataProvider() {
        return dh;
    }


    public void setExpressions(List<ExpressionEntry> entries) {
        db.setExpressions(entries);
    }

    /**
     * Persists the current variable database to disk. DataHub is not saved directly — mappings are derived from the database entries and get rebuilt
     * by {@link #revalidateRows()}, which the caller should invoke after {@link #load()} on startup.
     */
    public void save() throws IOException {
        db.save(SAVE_FILE);
    }

    /**
     * Restores the variable database from the last {@link #save()}. Does nothing if no save file exists yet (e.g. first launch).
     * Deserialized {@link ExpressionNode}s lose transient DataProvider, so each entry's expression is re-parsed to wire it back to DataHub.
     */
    public void load() throws IOException, ClassNotFoundException {
        if (!Files.exists(SAVE_FILE)) {
            return;
        }
        db.load(SAVE_FILE);
        rewireLoadedExpressions();
    }

    private void rewireLoadedExpressions() {
        List<ExpressionEntry> entries = db.getExpressions();
        for (int i = 0; i < entries.size(); i++) {
            ExpressionEntry entry = entries.get(i);
            if (entry.variableType().equals("Updatable Variable")) {
                continue;
            }
            try {
                ExpressionNode node = parseExpression(entry.expression(), getExpressionTypeByName(i));
                //node is the same, but now DataHub provided to all variable nodes within the expression.
                db.setExpression(i, new ExpressionEntry(entry.name(), entry.expression(), node, entry.variableType(), entry.defaultValue(), entry.comment()));
            } catch (Exception ignored) {
                //leave the stored node unwired; revalidateRows() will flag this row as invalid
            }
        }
    }


    /**
     * Panels use this method to add rows to the VariableDataBase, indices update accordingly.
     *
     * @param index
     */
    public void removeExpression(int index) {
        removeFromDataProvider(index);
        db.removeExpression(index);
    }
    /**
     * Makes sure no stale values are still available in DataHub.
     *
     * @param index
     */
    private void removeFromDataProvider(int index) {
        ExpressionEntry removal = db.getExpression(index);
        switch (removal.expressionNode().resultType()) {
            case ExpressionType.BOOLEAN -> dh.removeBoolean(removal.name());
            case ExpressionType.DATE -> dh.removeDate(removal.name());
            case ExpressionType.DOUBLE -> dh.removeDouble(removal.name());
            case ExpressionType.INTEGER -> dh.removeInt(removal.name());
            case ExpressionType.STRING -> dh.removeString(removal.name());
        }
    }

    /**
     * Panels use this method to get a single expression.
     *
     * @param index
     */
    public ExpressionEntry getExpression(int index) {
        return db.getExpression(index);
    }

    /**
     * Used to allow dragging and dropping.
     *
     * @param from
     * @param to
     */
    public void moveExpression(int from, int to) {
        db.moveExpression(from, to);
    }

    /**
     * Registers ExpressionEntry name to DataHub before adding to Database.
     */
    public void putExpression(EditEvent ev) throws Exception {
        int index = db.findName(ev.getName());
        ExpressionEntry entry = new ExpressionEntry(ev.getName(), ev.getExpressionString(), ev.getExpression(), ev.getVariableType(), ev.getDefaultValue(), ev.getComment());
        Object value = entry.variableType().equals("Updatable Variable") ? entry.defaultValue() : evaluateSafely(entry.expressionNode());
        registerInDataHub(ev.getName(), entry.expressionNode().resultType(), value);
        if (index != -1) {
            db.setExpression(index, entry);
        } else {
            db.addExpression(entry);
        }
    }

    /**
     * Builds the {@link EditEvent} for the Add/Edit Variable dialog's form input: parses the typed expression (or default value) and picks the matching placeholder {@link ExpressionNode} type.
     *
     * @return the event to pass to {@link #putExpression}, or {@code null} if there is nothing to save
     */
    public EditEvent createEditEvent(Object source, String name, String expressionText, String comment, String variableType, String defaultValueText) throws Exception {
        ExpressionNode defaultValueEvaluate;
        try {
            defaultValueEvaluate = defaultValueText.isEmpty() ? new DoubleConstantNode(0.0) : parseExpression(defaultValueText);
        } catch (Exception illegalExpression) {
            return null;
        }
        Object defaultValue;
        ExpressionNode newExp;

        if (!expressionText.isEmpty() && !variableType.equals("Updatable Variable")) {
            try {
                newExp = parseExpression(expressionText);
                defaultValue = "";
            } catch (Exception ignored) {
                return null;
            }
        } else if (variableType.equals("Updatable Variable")) {
            switch (defaultValueEvaluate.resultType()) {
                //These don't actually create usable VariableNodes, constructed here for clarification on what kind of node is being added
                case ExpressionType.DOUBLE -> newExp = new DoubleVariableNode("");
                case ExpressionType.BOOLEAN -> newExp = new BooleanVariableNode("");
                case ExpressionType.INTEGER -> newExp = new IntegerVariableNode("");
                case ExpressionType.STRING -> newExp = new StringVariableNode("");
                case ExpressionType.DATE -> newExp = new DateTimeVariableNode("");
                default -> throw new RuntimeException("Invalid Default Value");
            }
            expressionText = "[" + name + "]";
            defaultValue = evaluateSafely(defaultValueEvaluate);
            if (defaultValue == null) {
                return null;
            }
        } else {
            return null;
        }

        return new EditEvent(source, name, newExp, expressionText, variableType, defaultValue, comment);
    }

    /**
     * On compute time, create a {@link Map} that maps each expression entry's name to the {@link ExpressionType} to allow {@link ExpressionParser} to
     * build variable nodes with valid names.
     * @return
     */

    public Map<String, ExpressionType> getExpressionTypeByName() {
        return getExpressionTypeByName(db.getExpressions().size());
    }

    //Same as getExpressionTypeByName(), but only mapping the first `limit` entries. Builds the map incrementally,
    //top to bottom, so an entry that has a problem fails to parse against the names defined after it
    private Map<String, ExpressionType> getExpressionTypeByName(int limit) {
        List<ExpressionEntry> entries = db.getExpressions();
        Map<String, ExpressionType> variableMap = new HashMap<>();
        for (int i = 0; i < limit; i++) {
            ExpressionEntry entry = entries.get(i);
            if (entry.variableType().equals("Updatable Variable")) {
                variableMap.put(entry.name(), entry.expressionNode().resultType());
                continue;
            }
            try {
                ExpressionNode node = parseExpression(entry.expression(), variableMap);
                variableMap.put(entry.name(), node.resultType());
            } catch (Exception problem) {
                //this entry has a problem — leave it out of the map so that a parseError is returned during parsing after revalidation
            }
        }
        return variableMap;
    }

    //Only parse to update text, nodes are transient and not saved
    public ExpressionNode parseExpression(String text) throws Exception {
        return parseExpression(text, getExpressionTypeByName());
    }

    public ExpressionNode parseExpression(String text, Map<String, ExpressionType> variableMap) throws Exception {
        ParseResult result = ExpressionParser.parse(text, variableMap);
        if (result.isSuccess()) {
            ExpressionNode node = (ExpressionNode) result.getNode();
            //parse result creates new variableNodes, must set their providers after parsing
            node.setProvider(dh);
            return node;
        }
        throw new IllegalArgumentException(result.getError() + " at position " + result.getError().position());
    }

    /**
     * The compute pathway: walks every entry top to bottom, computing and registering each valid row's
     * fresh value into the shared DataHub so later rows can reference it by name. A row referencing a
     * variable not defined by an earlier row (deleted, or reordered to come after) is NOT computed —
     * any stale value it previously registered is removed instead, so anything depending on it fails
     * loudly (a missing DataHub entry). This is called after a row is added, edited, deleted, or moved.
     * Returns the indices of invalid rows.
     */
    public Set<Integer> revalidateRows() {
        List<ExpressionEntry> entries = db.getExpressions();
        Set<Integer> invalid = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            ExpressionEntry entry = entries.get(i);
            try {
                if (entry.variableType().equals("Updatable Variable")) {
                    //this is fine, defaultValue is only necessary during ExpressionBuilding, outside programs will use their own DataProvider to set entry's actual value.
                    registerInDataHub(entry.name(), entry.expressionNode().resultType(), entry.defaultValue());
                } else {
                    ExpressionNode node = parseExpression(entry.expression(), getExpressionTypeByName(i));
                    registerInDataHub(entry.name(), node.resultType(), evaluateSafely(node));
                }
            } catch (Exception e) {
                invalid.add(i);
                removeFromDataProvider(i);
            }
        }
        return invalid;
    }

    private void registerInDataHub(String name, ExpressionType type, Object value) {
        switch (type) {
            case ExpressionType.DOUBLE -> dh.setDouble(name, (double) value);
            case ExpressionType.BOOLEAN -> dh.setBoolean(name, (boolean) value);
            case ExpressionType.INTEGER -> dh.setInt(name, (int) value);
            case ExpressionType.STRING -> dh.setString(name, (String) value);
            case ExpressionType.DATE -> dh.setDate(name, (LocalDateTime) value);
        }
    }


    public Object evaluateSafely(ExpressionNode node) throws Exception {
        Method evalMethod = node.getClass().getMethod("evaluate");
        return evalMethod.invoke(node);
    }
}