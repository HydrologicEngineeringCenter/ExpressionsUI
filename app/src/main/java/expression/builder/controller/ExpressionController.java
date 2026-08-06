package expression.builder.controller;

import java.util.List;

import expression.builder.model.DataBase;
import expression.builder.model.ExpressionEntry;
import expression.builder.view.EditEvent;
import usace.hec.expressions.ExpressionNode;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * All panels access the database of {@link ExpressionEntry} through an instance of this class (can most likely be static).
 */
public class ExpressionController {
    DataBase db = new DataBase();

    /**
     * {@code db.getExpressions()} returns a pointer to the list of db, but the panels can't modify it.
     * @return
     */
    public List<ExpressionEntry> getExpressions(){return db.getExpressions();}

    public void setExpressions(List<ExpressionEntry> entries){db.setExpressions(entries);}



    /**
     * Panels use this method to add rows to the DataBase, indices update accordingly.
     * @param index
     */
    public void removeExpression(int index) {
        db.removeExpression(index);
    }

    public void putExpression(EditEvent ev){
        int index = db.findName(ev.getName());
        ExpressionEntry entry = new ExpressionEntry(ev.getName(), ev.getExpressionString(), ev.getExpression(), ev.getDefaultValue());
        if (index != -1){
            db.setExpression(index, entry);
        } else{
            db.addExpression(entry);
        }
    }

    /**
     * On compute time, create a {@link Map} that maps each expression entry's name to the {@link ExpressionNode} to allow for any ExpressionNode to refer to other ExpressionNode's by name.
     * @return
     */
    public Map<String, ExpressionNode> getExpressionNodesByName() {
        List<ExpressionEntry> myList = db.getExpressions();
        return db.getExpressions().stream().limit(myList.size() - 1)
                .collect(Collectors.toMap(entry -> entry.getName(), entry-> entry.getExpressionNode()));
    }
}
