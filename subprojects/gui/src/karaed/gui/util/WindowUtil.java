package karaed.gui.util;

import javax.swing.*;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BooleanSupplier;

final class WindowUtil {

    private static final ImageIcon WINDOW_ICON = InputUtil.getIcon("/karaed.png");

    static void initWindow(Window window, BooleanSupplier onClosing) {
        window.setIconImage(WINDOW_ICON.getImage());
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
