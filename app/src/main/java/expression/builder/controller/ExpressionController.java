package expression.builder.controller;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import expression.builder.model.VariableDataBase;
import expression.builder.model.ExpressionEntry;
import expression.builder.view.EditEvent;
import usace.hec.expressions.*;
import usace.hec.model.DataHub;

import java.util.Map;
import java.util.stream.Collectors;

/**
 *  @apiNote Internal wiring — not intended for use outside this application.
 * All panels access the database of {@link ExpressionEntry} through an instance of this class (can most likely be static).
 */
public class ExpressionController {
    VariableDataBase db = new VariableDataBase();

    /**
     * {@code db.getExpressions()} returns a pointer to the list of db, but the panels can't modify it.
     * @return
     */
    public List<ExpressionEntry> getExpressions(){return db.getExpressions();}

    public void setExpressions(List<ExpressionEntry> entries){db.setExpressions(entries);}



    /**
     * Panels use this method to add rows to the VariableDataBase, indices update accordingly.
     * @param index
     */
    public void removeExpression(int index) {
        db.removeExpression(index);
    }

    public void putExpression(EditEvent ev){
        int index = db.findName(ev.getName());
        ExpressionEntry entry = new ExpressionEntry(ev.getName(), ev.getExpressionString(), ev.getExpression(), ev.getVariableType(), ev.getDefaultValue());
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
    //TODO: implementation currently does not use map the last entry to use it to return finalized value. Allow users to choose variable to return later
    public Map<String, ExpressionNode> getExpressionNodesByName() {
        List<ExpressionEntry> myList = db.getExpressions();
        return db.getExpressions().stream().limit(myList.size())
                .collect(Collectors.toMap(entry -> entry.name(), entry-> entry.expressionNode()));
    }

    public Map<String, ExpressionType> placeholder() {
        List<ExpressionEntry> myList = db.getExpressions();
        return db.getExpressions().stream().limit(myList.size()).filter(entry -> entry.variableType().equals("Updatable Variable"))
                .collect(Collectors.toMap(entry -> entry.name(), entry-> entry.expressionNode().resultType()));
    }

    public ExpressionNode parseExpression(String text) throws Exception {
        ExpressionParser parser = new ExpressionParser();
        //TODO: pass in Map from the VariableTable
        ParseResult result = parser.parse(text, placeholder());
        List<ExpressionEntry> data = getExpressions();
        DataHub dh = new DataHub();
        for(ExpressionEntry e:data){
            if(e.expressionNode() instanceof DataRequester){
                dh.setDouble(((DataRequester)e.expressionNode()).getName(), (double) e.defaultValue());
            }
        }
        if (result.isSuccess()) {
            ExpressionNode node = (ExpressionNode)result.getNode();
            node.setProvider(dh);
            return node;
        }
        throw new IllegalArgumentException(result.getError() + " at position " + result.getError().position());
    }

    public Object evaluateSafely(ExpressionNode node) throws Exception {
        Method evalMethod = node.getClass().getMethod("evaluate");
        return evalMethod.invoke(node);
    }
}
