package expression.builder.util;

public final class ExpressionFormatter {

    private ExpressionFormatter() {
    }

    /**
     * Reformats a flat expression string onto multiple lines, indenting one level deeper
     * after each '(' and one level shallower before each ')', with each ',' starting a new line.
     */
    public static String scriptPrint(String text) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (char c : text.toCharArray()) {
            if (c == '(') {
                sb.append(c);
                depth++;
                sb.append('\n').append("\t".repeat(depth));
            } else if (c == ')') {
                depth--;
                sb.append('\n').append("\t".repeat(Math.max(0, depth)));
                sb.append(c);
            } else if (c == ',') {
                sb.append(c);
                sb.append('\n').append("\t".repeat(Math.max(0, depth)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
