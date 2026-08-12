package expression.builder.controller;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
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
    DataHub dh = new DataHub();

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

    public void putExpression(EditEvent ev) throws Exception {
        int index = db.findName(ev.getName());
        ExpressionEntry entry = new ExpressionEntry(ev.getName(), ev.getExpressionString(), ev.getExpression(), ev.getVariableType(), ev.getDefaultValue());
        //Initialize ExpressionNode
        switch (entry.expressionNode().resultType()) {
                case ExpressionType.DOUBLE -> dh.setDouble(ev.getName(), entry.variableType().equals("Updatable Variable")? (double) entry.defaultValue() : (double) evaluateSafely(entry.expressionNode()));
                case ExpressionType.BOOLEAN -> dh.setBoolean(ev.getName(), entry.variableType().equals("Updatable Variable")? (boolean) entry.defaultValue(): (boolean) evaluateSafely(entry.expressionNode()));
                case ExpressionType.INTEGER -> dh.setInt(ev.getName(), entry.variableType().equals("Updatable Variable")? (int) entry.defaultValue() : (int) evaluateSafely(entry.expressionNode()));
                case ExpressionType.STRING -> dh.setString(ev.getName(), entry.variableType().equals("Updatable Variable")? (String) entry.defaultValue() : (String) evaluateSafely(entry.expressionNode()));
                case ExpressionType.DATE -> dh.setDate(ev.getName(), entry.variableType().equals("Updatable Variable")? (LocalDateTime) entry.defaultValue() : (LocalDateTime) evaluateSafely(entry.expressionNode()));
        }
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
        return db.getExpressions().stream().limit(myList.size())
                .collect(Collectors.toMap(entry -> entry.name(), entry-> entry.expressionNode().resultType()));
    }

    //Only parse to update text, nodes are transient and not saved
    public ExpressionNode parseExpression(String text) throws Exception {
        Map<String, ExpressionType> variableMap = placeholder();
        ParseResult result = ExpressionParser.parse(text, variableMap);
        if (result.isSuccess()) {
            ExpressionNode node = (ExpressionNode)result.getNode();
            //parse result creates new variableNodes, must set their providers after parsing
            node.setProvider(dh);
            return node;
        }
        throw new IllegalArgumentException(result.getError() + " at position " + result.getError().position());
    }

//    public void simulateVariables() throws Exception {
//        List<ExpressionEntry> data = getExpressions();
//        //TODO: move DataHub to register when expressions are ADDED, not parsed. Also update it so that ALL types are supported.
//        for(ExpressionEntry e : data){
//            if(e.expressionNode() instanceof DataRequester){
//                ExpressionType eType = e.expressionNode().resultType();
//                switch (eType) {
//                    case ExpressionType.DOUBLE -> dh.setDouble(((DataRequester)e.expressionNode()).getName(), (double) e.defaultValue());
//                    case ExpressionType.BOOLEAN -> dh.setBoolean(((DataRequester)e.expressionNode()).getName(), (boolean) e.defaultValue());
//                    case ExpressionType.INTEGER -> dh.setInt(((DataRequester)e.expressionNode()).getName(), (int) e.defaultValue());
//                    case ExpressionType.STRING -> dh.setString(((DataRequester)e.expressionNode()).getName(), (String) e.defaultValue());
//                    case ExpressionType.DATE -> dh.setDate(((DataRequester)e.expressionNode()).getName(), (LocalDateTime) e.defaultValue());
//                }
//                dh.setDouble(((DataRequester)e.expressionNode()).getName(), (double) e.defaultValue());
//            }
//        }
//    }

    public Object evaluateSafely(ExpressionNode node) throws Exception {
        Method evalMethod = node.getClass().getMethod("evaluate");
        return evalMethod.invoke(node);
    }
}
