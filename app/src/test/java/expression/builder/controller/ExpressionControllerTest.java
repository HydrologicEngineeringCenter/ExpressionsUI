package expression.builder.controller;

import expression.builder.model.ExpressionEntry;
import expression.builder.view.EditEvent;
import org.junit.jupiter.api.Test;
import usace.hec.expressions.DoubleConstantNode;
import usace.hec.expressions.ExpressionType;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Series of sanity tests for whenever ExpressionController logic is edited
 */
class ExpressionControllerTest {

    @Test
    void updatableVariableWithBlankDefaultBuildsDoubleZeroPlaceholder() throws Exception {
        ExpressionController controller = new ExpressionController();

        EditEvent ev = controller.createEditEvent(this, "myVar", "", "a comment", "Updatable Variable", "");

        assertEquals("myVar", ev.getName());
        assertEquals("[myVar]", ev.getExpressionString());
        assertEquals("Updatable Variable", ev.getVariableType());
        assertEquals("a comment", ev.getComment());
        assertEquals(ExpressionType.DOUBLE, ev.getExpression().resultType());
        assertEquals(0.0, ev.getDefaultValue());
    }

    @Test
    void nonUpdatableVariableWithBlankExpressionReturnsNull() throws Exception {
        ExpressionController controller = new ExpressionController();

        EditEvent ev = controller.createEditEvent(this, "myVar", "", "a comment", "Constant", "");

        assertNull(ev);
    }

    @Test
    void emptyListHasNoInvalidRows() {
        ExpressionController controller = new ExpressionController();
        controller.setExpressions(List.of());

        assertEquals(Set.of(), controller.revalidateRows());
    }

    @Test
    void revalidateRowsRegistersUpdatableVariableValueInDataHub() throws Exception {
        ExpressionController controller = new ExpressionController();
        EditEvent ev = controller.createEditEvent(this, "x", "", "", "Updatable Variable", "");
        controller.putExpression(ev);

        Set<Integer> invalid = controller.revalidateRows();

        assertEquals(Set.of(), invalid);
        assertEquals(0.0, controller.dh.provideDouble("x"));
    }

    @Test
    void rowReferencingUndefinedNameIsInvalid() {
        ExpressionController controller = new ExpressionController();
        controller.setExpressions(List.of(
                new ExpressionEntry("total", "[doesNotExist]", new DoubleConstantNode(0.0), "Constant", "", "")
        ));

        assertEquals(Set.of(0), controller.revalidateRows());
    }

    @Test
    void rowReferencingAnInvalidRowIsAlsoInvalid() {
        ExpressionController controller = new ExpressionController();
        controller.setExpressions(List.of(
                new ExpressionEntry("b", "[doesNotExist]", new DoubleConstantNode(0.0), "Constant", "", ""),
                new ExpressionEntry("c", "[b]", new DoubleConstantNode(0.0), "Constant", "", "")
        ));

        assertEquals(Set.of(0, 1), controller.revalidateRows());
    }

    @Test
    void invalidRowClearsAnyPreviouslyRegisteredValue() {
        ExpressionController controller = new ExpressionController();
        controller.setExpressions(List.of(
                new ExpressionEntry("total", "[doesNotExist]", new DoubleConstantNode(0.0), "Constant", "", "")
        ));
        controller.dh.setDouble("total", 99.0); // simulate a stale value left over from before this row broke

        Set<Integer> invalid = controller.revalidateRows();

        assertEquals(Set.of(0), invalid);
        assertThrows(NullPointerException.class, () -> controller.dh.provideDouble("total"));
    }
}
