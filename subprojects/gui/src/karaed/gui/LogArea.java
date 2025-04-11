package karaed.gui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.util.regex.Pattern;

final class LogArea {

    private static final Pattern ESC_SEQUENCE = Pattern.compile("\u001b\\[\\dm");

    private final JTextArea taLog = new JTextArea(10, 80);

    LogArea() {
        taLog.setEditable(false);
        taLog.setLineWrap(true);
    }

    private void appendLine(String line) {
        boolean cr = false;
        while (true) {
            int p = line.lastIndexOf('\r');
            if (p < 0)
                break;
            if (p == line.length() - 1) {
                line = line.substring(0, p);
            } else {
                line = line.substring(p + 1);
                cr = true;
                break;
            }
        }
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
