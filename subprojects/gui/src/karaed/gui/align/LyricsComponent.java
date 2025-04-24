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
        InputUtil.undoable(taLines);

        AbstractDocument document = (AbstractDocument) taLines.getDocument();
        document.setDocumentFilter(new DocumentFilter() {

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                fb.insertString(offset, string, attr);
                recolor();
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                fb.replace(offset, length, text, attrs);
                recolor();
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                String insert = null;
                if (length == 1) {
                    String text = fb.getDocument().getText(offset, length);
                    if ("\n".equals(text)) {
                        insert = " ";
                        SwingUtilities.invokeLater(() -> end());
                    }
                }
                if (insert != null) {
                    fb.replace(offset, length, insert, SimpleAttributeSet.EMPTY);
                } else {
                    fb.remove(offset, length);
                }
                recolor();
            }
        });
        recolor();
    }

    private interface LineConsumer {

        boolean consume(int lineIndex, int lineStart, int lineEnd, String line) throws BadLocationException;
    }

    private int scanLines(LineConsumer consumer) {
        int count = 0;
        int lines = taLines.getLineCount();
        for (int i = 0; i < lines; i++) {
            try {
                int lineStart = taLines.getLineStartOffset(i);
                int lineEnd = taLines.getLineEndOffset(i);
                String line = taLines.getText(lineStart, lineEnd - lineStart);
                if (line.trim().isEmpty())
                    continue;
                if (consumer != null) {
                    if (!consumer.consume(count, lineStart, lineEnd, line))
                        break;
                }
            } catch (BadLocationException ex) {
                // ignore
            }
            count++;
        }
        return count;
    }

    void recolor() {
        Highlighter hl = taLines.getHighlighter();
        MyPainter.removeMyHighlights(hl);

        int count = scanLines(null);
        if (count != lineCount) {
            lineCount = count;
            for (Runnable listener : linesChanged) {
                listener.run();
            }
        }
        scanLines((lineIndex, lineStart, lineEnd, line) -> {
            Color color = colors.getColor(lineIndex);
            if (color != null) {
                hl.addHighlight(lineStart, lineEnd, new MyPainter(color));
            }
            return true;
        });
    }

    private void end() {
        try {
            int caret = taLines.getCaretPosition();
            int line = taLines.getLineOfOffset(caret);
            int end = taLines.getLineEndOffset(line);
            taLines.setCaretPosition(end - 1);
        } catch (BadLocationException ex) {
            // ignore
        }
    }

    void setLines(List<String> text) {
        InputUtil.setText(taLines, String.join("\n", text) + "\n");
    }

    Document getDocument() {
        return taLines.getDocument();
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

    String getLineAt(int index) {
        String[] found = new String[1];
        scanLines((lineIndex, lineStart, lineEnd, line) -> {
            if (lineIndex == index) {
                found[0] = line;
                return false;
            }
            return true;
        });
        return found[0];
    }

    void goTo(int index) {
        int[] found = {-1};
        scanLines((lineIndex, lineStart, lineEnd, line) -> {
            if (lineIndex == index) {
                found[0] = lineStart;
                return false;
            }
            return true;
        });
        if (found[0] >= 0) {
            taLines.setCaretPosition(found[0]);
            SwingUtilities.invokeLater(taLines::requestFocusInWindow);
        }
    }

    void addLinesChanged(Runnable listener) {
        linesChanged.add(listener);
    }
}
