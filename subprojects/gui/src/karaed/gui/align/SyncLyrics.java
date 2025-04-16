package karaed.gui.align;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import karaed.engine.ass.AssUtil;
import karaed.engine.steps.align.Align;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class SyncLyrics {

    private static final MyPainter INSERT = new MyPainter(new Color(150, 255, 150));
    private static final MyPainter DELETE = new MyPainter(new Color(255, 150, 150));
    private static final MyPainter CHANGE = new MyPainter(new Color(150, 150, 255));

    private final JTextArea taRanges;
    private final JTextArea taText;
    private final JSplitPane split;

    private boolean ignoreRangeScroll = false;
    private boolean ignoreTextScroll = false;
    private boolean aligned = false;

    SyncLyrics(Document rangesDocument, String text, Runnable onChange) {
        this.taRanges = new JTextArea(25, 30);
        this.taText = new JTextArea(25, 30);
        JScrollPane rangesScroll = new JScrollPane(taRanges);
        JScrollPane textScroll = new JScrollPane(taText);
        this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rangesScroll, textScroll);

        split.setDividerLocation(0.5);
        split.setResizeWeight(0.5);

        taRanges.setLineWrap(true);
        taRanges.setWrapStyleWord(true);
        taText.setLineWrap(true);
        taText.setWrapStyleWord(true);

        taRanges.setDocument(rangesDocument);
        taRanges.setEditable(false);
        taText.setText(text);
        InputUtil.undoable(taText);

        DocumentListener listener = new DocumentListener() {

            private void changed() {
                onChange.run();
                paintDiffs();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        };
        taRanges.getDocument().addDocumentListener(listener);
        taText.getDocument().addDocumentListener(listener);
        paintDiffs();

        JScrollBar rangesBar = rangesScroll.getVerticalScrollBar();
        JScrollBar textBar = textScroll.getVerticalScrollBar();
        rangesBar.addAdjustmentListener(e -> {
            if (ignoreRangeScroll)
                return;
            ignoreRangeScroll = true;
            try {
                textBar.setValue(convertValue(e.getValue(), rangesBar, textBar));
            } finally {
                ignoreRangeScroll = false;
            }
        });
        textBar.addAdjustmentListener(e -> {
            if (ignoreTextScroll)
                return;
            int value = e.getValue();
            ignoreTextScroll = true;
            try {
                rangesBar.setValue(convertValue(value, textBar, rangesBar));
            } finally {
                ignoreTextScroll = false;
            }
        });
    }

    private static int convertValue(int value, JScrollBar from, JScrollBar to) {
        double ratio = (double) (value - from.getMinimum()) / (from.getMaximum() - from.getMinimum());
        return (int) Math.round(to.getMinimum() + (to.getMaximum() - to.getMinimum()) * ratio);
    }

    private record LinedChar(
        int line,
        int col,
        char ch
    ) {

        boolean isFake() {
            return line < 0;
        }

        @Override
        public int hashCode() {
            return ch;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LinedChar that && this.ch == that.ch;
        }
    }

    private static List<LinedChar> toLined(String text) {
        List<LinedChar> chars = new ArrayList<>();
        text.lines().forEachOrdered(new Consumer<>() {

            int i = 0;

            @Override
            public void accept(String line) {
                if (!(line.trim().isEmpty() || Align.isNonVocalLine(line))) {
                    if (!chars.isEmpty()) {
                        chars.add(new LinedChar(-1, -1, ' '));
                    }
                    int prevWordEnd = -1;
                    boolean word = false;
                    for (int j = 0; j < line.length(); j++) {
                        char ch = line.charAt(j);
                        if (AssUtil.isLetter(ch)) {
                            if (!word && prevWordEnd >= 0) {
                                chars.add(new LinedChar(i, prevWordEnd, ' '));
                            }
                            chars.add(new LinedChar(i, j, ch));
                            word = true;
                        } else {
                            if (word) {
                                prevWordEnd = j;
                            }
                            word = false;
                        }
                    }
                }
                i++;
            }
        });
        return chars;
    }

    private static LinedChar first(List<LinedChar> chars) {
        for (LinedChar ch : chars) {
            if (!ch.isFake())
                return ch;
        }
        return null;
    }

    private static LinedChar last(List<LinedChar> chars) {
        return first(chars.reversed());
    }

    private static int position(JTextArea ta, LinedChar ch) {
        if (ch == null)
            return -1;
        try {
            int lineStart = ta.getLineStartOffset(ch.line());
            return lineStart + ch.col;
        } catch (BadLocationException ex) {
            return -1;
        }
    }

    private static void addHighlight(JTextArea ta, Highlighter highlighter, DeltaType type, Chunk<LinedChar> chunk) {
        List<LinedChar> lines = chunk.getLines();
        int first = position(ta, first(lines));
        int last = position(ta, last(lines));
        if (first < 0 || last < 0)
            return;
        Highlighter.HighlightPainter painter = switch (type) {
            case INSERT -> INSERT;
            case DELETE -> DELETE;
            case CHANGE -> CHANGE;
            case EQUAL -> null;
        };
        if (painter == null)
            return;
        try {
            highlighter.addHighlight(first, last + 1, painter);
        } catch (BadLocationException ex) {
            // ignore
        }
    }

    private void paintDiffs() {
        Highlighter hr = taRanges.getHighlighter();
        MyPainter.removeMyHighlights(hr);
        Highlighter ht = taText.getHighlighter();
        MyPainter.removeMyHighlights(ht);

        List<LinedChar> rangesChars = toLined(taRanges.getText());
        List<LinedChar> textChars = toLined(taText.getText());
        Patch<LinedChar> patch = DiffUtils.diff(textChars, rangesChars, false);
        boolean hasDiff = false;
        for (AbstractDelta<LinedChar> delta : patch.getDeltas()) {
            DeltaType type = delta.getType();
            if (type == DeltaType.INSERT || type == DeltaType.CHANGE) {
                hasDiff = true;
                addHighlight(taRanges, hr, type, delta.getTarget());
            }
            if (type == DeltaType.DELETE || type == DeltaType.CHANGE) {
                hasDiff = true;
                addHighlight(taText, ht, type, delta.getSource());
            }
        }
        this.aligned = !hasDiff;
    }

    JComponent getVisual() {
        return split;
    }

    boolean isAligned() {
        return aligned;
    }

    List<String> getText() {
        return taText.getText().lines().toList();
    }
}
