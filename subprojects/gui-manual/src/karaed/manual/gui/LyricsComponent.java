package karaed.manual.gui;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

final class LyricsComponent {

    private final ColorSequence colors;
    private final JPanel main = new JPanel();
    private final List<JTextField> lines = new ArrayList<>();

    private final List<Runnable> linesChanged = new ArrayList<>();

    private final AbstractAction merge = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField tf = (JTextField) e.getSource();
            merge(tf);
        }
    };
    private final AbstractAction split = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField tf = (JTextField) e.getSource();
            split(tf);
        }
    };

    private static final class ReadonlyDocument extends PlainDocument {

        boolean frozen = false;

        @Override
        public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            if (!frozen) {
                super.insertString(offs, str, a);
            }
        }

        @Override
        public void remove(int offs, int len) throws BadLocationException {
            if (!frozen) {
                super.remove(offs, len);
            }
        }

        @Override
        public void replace(int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (!frozen) {
                super.replace(offset, length, text, attrs);
            }
        }
    }

    private JTextField newField(String text) {
        ReadonlyDocument doc = new ReadonlyDocument();
        JTextField tf = new JTextField(doc, text, 0);
        tf.getActionMap().put("deleteMerge", merge);
        tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteMerge");
        tf.getActionMap().put("enterSplit", split);
        tf.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterSplit");
        doc.frozen = true;
        return tf;
    }

    LyricsComponent(ColorSequence colors) {
        this.colors = colors;
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
    }

    void setLines(List<String> text) {
        lines.clear();
        main.removeAll();
        for (String line : text) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;
            JTextField tf = newField(trimmed);
            lines.add(tf);
            main.add(tf);
        }
        recolor();
        main.revalidate();
    }

    private void split(JTextField tf) {
        int cp = tf.getCaretPosition();
        if (cp <= 0 || cp >= tf.getDocument().getLength())
            return;
        int index = lines.indexOf(tf);
        if (index < 0)
            return;

        String fullLine = tf.getText();
        String line = fullLine.substring(0, cp).trim();
        String nextLine = fullLine.substring(cp).trim();
        // todo: cannot split in the middle of the word!!!

        ReadonlyDocument doc = (ReadonlyDocument) tf.getDocument();
        doc.frozen = false;
        tf.setText(line);
        doc.frozen = true;

        JTextField next = newField(nextLine);
        lines.add(index + 1, next);
        main.add(next, index + 1);
        redraw();
    }

    private void merge(JTextField tf) {
        int cp = tf.getCaretPosition();
        if (cp < tf.getDocument().getLength())
            return;
        int index = lines.indexOf(tf);
        if (index < 0 || index >= lines.size() - 1)
            return;
        JTextField next = lines.get(index + 1);

        String nextLine = next.getText();
        ReadonlyDocument doc = (ReadonlyDocument) tf.getDocument();
        doc.frozen = false;
        String line = tf.getText();
        tf.setText(line + " " + nextLine);
        doc.frozen = true;

        main.remove(next);
        lines.remove(index + 1);
        redraw();
    }

    void recolor() {
        for (int i = 0; i < lines.size(); i++) {
            JTextField line = lines.get(i);
            Color color = colors.getColor(i);
            line.setBackground(color == null ? Color.white : color);
        }
    }

    private void redraw() {
        recolor();
        main.revalidate();
        for (Runnable runnable : linesChanged) {
            runnable.run();
        }
    }

    JComponent getVisual() {
        return main;
    }

    int getLineCount() {
        return lines.size();
    }

    List<String> getLines() {
        List<String> text = new ArrayList<>(lines.size());
        for (JTextField line : lines) {
            text.add(line.getText());
        }
        return text;
    }

    void addLinesChanged(Runnable listener) {
        linesChanged.add(listener);
    }
}
