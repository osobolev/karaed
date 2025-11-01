package karaed.gui.tools;

import karaed.gui.util.BaseDialog;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.LogArea;
import karaed.tools.OutputCapture;

import javax.swing.*;
import java.awt.BorderLayout;

final class LazyLogDialog implements OutputCapture {

    private final BaseWindow owner;
    private final Thread thread;

    private volatile LogDialog dialog = null;

    LazyLogDialog(BaseWindow owner, Thread thread) {
        this.owner = owner;
        this.thread = thread;
    }

    @Override
    public void output(boolean stderr, String text) {
        synchronized (this) {
            if (dialog == null) {
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        dialog = new LogDialog(owner, thread);
                        SwingUtilities.invokeLater(() -> dialog.setVisible(true));
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        SwingUtilities.invokeLater(() -> dialog.logArea.append(stderr, text));
    }

    void close() {
        if (dialog != null) {
            dialog.dispose();
        }
    }

    private static final class LogDialog extends BaseDialog {

        private final Thread thread;
        final LogArea logArea = new LogArea();

        LogDialog(BaseWindow owner, Thread thread) {
            super(owner.toWindow(), owner.getLogger(), "Log");
            this.thread = thread;

            add(new JScrollPane(logArea.getVisual()), BorderLayout.CENTER);

            pack();
            setLocationRelativeTo(null);
        }

        @Override
        public boolean onClosing() {
            thread.interrupt();
            return false;
        }
    }
}
