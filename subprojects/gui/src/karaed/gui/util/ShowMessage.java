package karaed.gui.util;

import karaed.gui.ErrorLogger;

import javax.swing.*;
import java.awt.Window;

public final class ShowMessage {

    public static void error(ErrorLogger logger, Window window, Throwable ex) {
        logger.error(ex);
        error(window, ex.toString());
    }

    public static void error(Window window, String message) {
        JOptionPane.showMessageDialog(window, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm2(Window window, String message) {
        int ans = JOptionPane.showConfirmDialog(
            window, message, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        return ans == JOptionPane.YES_OPTION;
    }
}
