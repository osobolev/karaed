package karaed.gui.util;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.BooleanSupplier;

final class CloseUtil {

    static void listen(Window window, BooleanSupplier onClosing) {
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
