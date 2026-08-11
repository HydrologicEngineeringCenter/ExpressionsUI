package expression.builder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *  @apiNote Internal wiring — not intended for use outside this application.
 * Stores a List of {@link ExpressionEntry}s as the backend for the {@link VariableTableModel in the expressions tab. This VariableDataBase can
 * only be updated through {@link expression.builder.controller.ExpressionController}, as it passes in an immutable List to other panels that want to
 * access its contents.
 */
public class VariableDataBase {
    private List<ExpressionEntry> expressions;

    public VariableDataBase(){
        expressions = new ArrayList<>();
    }

    //when adding new rows, calls this function to expand the list and to add a new row to the table.
    public void addExpression(ExpressionEntry expression){
        expressions.add(expression);
    }

    //when editing rows, change the entry in the row accordingly
    public void setExpression(int index, ExpressionEntry expression) {
        expressions.set(index, expression);
    }

    //restore saved entries list after application has closed.
    public void setExpressions(List<ExpressionEntry> entries){
        expressions = new ArrayList<>();
        if (entries == null || entries.isEmpty())
        {
            return;
        }
        expressions.addAll(entries);
    }

    //popup menu to remove rows in the table model
    public void removeExpression(int index){
        expressions.remove(index);
    }

    /**
     * allows other Panels to access the array but not modify them, all components will have to use {@link expression.builder.controller.ExpressionController}
     * to modify VariableDataBase
     */
    public List<ExpressionEntry> getExpressions(){
        return Collections.unmodifiableList(expressions);
    }

    public int findName(String name){
        for (int i = 0; i < expressions.size(); i++){
            if (expressions.get(i).name().equals(name)){
                return i;
            }
        }
        return -1;
    }
}
