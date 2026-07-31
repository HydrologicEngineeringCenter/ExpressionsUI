package expression.builder;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import usace.hec.expressions.DisplayNode;

public class ExpressionNodeTextBox extends JPanel {
    private final JTextArea textArea;
    private boolean isProgrammaticUpdate = false;

    public interface TextUpdateListener {
        void onTextUpdated(String text);
    }

    private TextUpdateListener textUpdateListener;

    public ExpressionNodeTextBox() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Expression Preview"));

        textArea = new JTextArea();
        textArea.setEditable(true);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setBackground(new Color(245, 245, 245));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        textArea.setText("");

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { notifyUpdate(); }
            @Override public void removeUpdate(DocumentEvent e) { notifyUpdate(); }
            @Override public void changedUpdate(DocumentEvent e) { notifyUpdate(); }

            private void notifyUpdate() {
                if (!isProgrammaticUpdate && textUpdateListener != null) {
                    String text = textArea.getText().trim();
                    textUpdateListener.onTextUpdated(text);
                }
            }
        });

        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void setNodeText(DisplayNode node) {
        isProgrammaticUpdate = true;
        String syntax = node != null ? node.defaultSyntax(false) : "";
        textArea.setText(syntax);
        textArea.setCaretPosition(0);
        isProgrammaticUpdate = false;
    }

    public void insertNodeAtCursor(DisplayNode node) {
        if (node == null) return;
        String syntax;
        try {
            syntax = node.defaultSyntax(false);
        } catch (Exception e) {
            syntax = node.displayName(false) + "()";
        }
        insertTextAtCursor(syntax);
    }

    /**
     * Inserts a raw syntax string at the cursor. Used as a fallback when
     * AST node instantiation fails, keeping the UI resilient.
     */
    public void insertTextAtCursor(String text) {
        if (text == null || text.isEmpty()) return;
        isProgrammaticUpdate = true;
        int caretPos = textArea.getCaretPosition();
        String existing = textArea.getText();
        
        if (caretPos > 0 && !existing.isEmpty()) {
            char prevChar = existing.charAt(caretPos - 1);
            if (!Character.isWhitespace(prevChar)) {
                text = " " + text;
            }
        }
        
        textArea.replaceRange(text, caretPos, caretPos);
        textArea.setCaretPosition(caretPos + text.length());
        textArea.requestFocusInWindow();
        isProgrammaticUpdate = false;
    }

    public String getExpression() {
        return textArea.getText();
    }

    public void setTextUpdateListener(TextUpdateListener listener) {
        this.textUpdateListener = listener;
    }
}