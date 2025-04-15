package karaed.gui.align;

import karaed.gui.util.InputUtil;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

final class LyricsComponent {

    private final ColorSequence colors;
    private final JTextArea taLines = new JTextArea(25, 60);
    private final JScrollPane sp = new JScrollPane(taLines);

    private int lineCount = 0;

    private final List<Runnable> linesChanged = new ArrayList<>();

    LyricsComponent(ColorSequence colors) {
        this.colors = colors;

        taLines.setLineWrap(true);
        taLines.setWrapStyleWord(true);

        AbstractDocument document = (AbstractDocument) taLines.getDocument();
        document.setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                super.insertString(fb, offset, string, attr);
                recolor();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                super.replace(fb, offset, length, text, attrs);
                recolor();
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (length == 1) {
                    String text = fb.getDocument().getText(offset, length);
                    if ("\n".equals(text)) {
                        SwingUtilities.invokeLater(() -> end());
                    }
                }
                super.remove(fb, offset, length);
                recolor();
            }
        });
        recolor();
    }

    void recolor() {
        Highlighter highlighter = taLines.getHighlighter();
        highlighter.removeAllHighlights();

        int il = 0;
        int count = 0;
        int lines = taLines.getLineCount();
        for (int i = 0; i < lines; i++) {
            try {
                int lineStart = taLines.getLineStartOffset(i);
                int lineEnd = taLines.getLineEndOffset(i);
                String line = taLines.getText(lineStart, lineEnd - lineStart);
                if (line.trim().isEmpty())
                    continue;
                Color color = colors.getColor(il++);
                if (color != null) {
                    highlighter.addHighlight(lineStart, lineEnd, new DefaultHighlighter.DefaultHighlightPainter(color));
                }
            } catch (BadLocationException ex) {
                // ignore
            }
            count++;
        }
        if (count != lineCount) {
            lineCount = count;
            for (Runnable listener : linesChanged) {
                listener.run();
            }
        }
    }

    private void end() {
        try {
            int caret = taLines.getCaretPosition();
            int line = taLines.getLineOfOffset(caret);
            int end = taLines.getLineEndOffset(line);
            taLines.setCaretPosition(end);
        } catch (BadLocationException ex) {
            // ignore
        }
    }

    void setLines(List<String> text) {
        InputUtil.setText(taLines, String.join("\n", text) + "\n");
        // todo: do not fire changes here!!!
    }

    JComponent getVisual() {
        return sp;
    }

    int getLineCount() {
        return lineCount;
    }

    List<String> getLines() {
        return taLines.getText().lines().toList();
    }

    void addLinesChanged(Runnable listener) {
        linesChanged.add(listener);
    }
}
