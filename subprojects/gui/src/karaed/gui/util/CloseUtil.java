package karaed.gui.util;

import javax.swing.*;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BooleanSupplier;

public final class CloseUtil {

    public static void listen(Window window, BooleanSupplier onClosing) {
        if (window instanceof JFrame frame) {
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        } else if (window instanceof JDialog dialog) {
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onClosing.getAsBoolean()) {
                    window.dispose();
                }
            }
        });
    }
}
