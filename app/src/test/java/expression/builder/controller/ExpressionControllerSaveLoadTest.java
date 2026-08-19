package expression.builder.controller;

import expression.builder.view.EditEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExpressionControllerSaveLoadTest {

    @Test
    void loadRewiresExpressionsSoDataHubMatchesPreSaveState(@TempDir Path tempDir) throws Exception {
        Path saveFile = tempDir.resolve("variables.ser");

        ExpressionController original = new ExpressionController();
        EditEvent xEv = original.createEditEvent(this, "x", "", "", "Updatable Variable", "5.0");
        original.putExpression(xEv);
        EditEvent yEv = original.createEditEvent(this, "y", "[x]", "", "Constant", "");
        original.putExpression(yEv);
        original.revalidateRows();

        double expectedX = original.dh.provideDouble("x");
        double expectedY = original.dh.provideDouble("y");
        assertEquals(expectedX, expectedY); // sanity: y is a pure passthrough of x

        original.save(saveFile);

        ExpressionController restored = new ExpressionController();
        restored.load(saveFile);
        //rewireLoadedExpressions() (run inside load()) only re-wires each entry's stored ExpressionNode;
        //DataHub itself stays empty until revalidateRows() re-registers values from it, same as startup.
        restored.revalidateRows();

        assertEquals(expectedX, restored.dh.provideDouble("x"));
        assertEquals(expectedY, restored.dh.provideDouble("y"));

        //the rewired node (not just the recomputed DataHub value) should be usable too — its
        //DataProvider came back transient/null from deserialization until rewireLoadedExpressions() ran.
        assertNotNull(restored.evaluateSafely(restored.getExpression(1).expressionNode()));
    }
}
