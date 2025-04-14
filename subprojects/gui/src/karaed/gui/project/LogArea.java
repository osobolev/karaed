package karaed.gui.project;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.util.regex.Pattern;

final class LogArea {

    private static final Pattern ESC_SEQUENCE = Pattern.compile("\u001b\\[\\dm");

    private final JTextArea taLog = new JTextArea(10, 80);

    private boolean lastCR = false;

    LogArea() {
        taLog.setEditable(false);
        taLog.setLineWrap(true);
    }

    void clear() {
        taLog.setText("");
        lastCR = false;
    }

    private void appendLine(String line) {
        if (line.isEmpty())
            return;
        boolean wasLastCR = lastCR;
        boolean newLastCR = false;
        while (line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
            newLastCR = true;
        }
        boolean cr;
        int p1 = line.lastIndexOf('\r');
        if (p1 >= 0) {
            cr = true;
            line = line.substring(p1 + 1);
        } else {
            cr = wasLastCR;
        }
        lastCR = newLastCR;

        line = ESC_SEQUENCE.matcher(line).replaceAll(""); // todo: change font???
        // todo: process stderr
        if (cr) {
            try {
                int length = taLog.getDocument().getLength();
                int lineIndex = taLog.getLineOfOffset(length);
                int lineStart = taLog.getLineStartOffset(lineIndex);
                taLog.replaceRange(line, lineStart, length);
            } catch (BadLocationException ex) {
                // ignore
            }
        } else {
            taLog.append(line);
        }
    }

    private void doAppend(boolean stderr, String text) {
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                appendLine(text.substring(start, i));
                taLog.append("\n");
                start = i + 1;
                lastCR = false;
            }
        }
        if (start < text.length()) {
            appendLine(text.substring(start));
        }
    }

    void append(boolean stderr, String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            doAppend(stderr, text);
        } else {
            SwingUtilities.invokeLater(() -> doAppend(stderr, text));
        }
    }

    JComponent getVisual() {
        return taLog;
    }
}
