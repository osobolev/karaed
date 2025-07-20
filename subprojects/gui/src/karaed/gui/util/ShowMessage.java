package karaed.gui.util;

import karaed.gui.ErrorLogger;

import javax.swing.*;
import java.awt.Component;

public final class ShowMessage {

    public static void error(ErrorLogger logger, Component comp, Throwable ex) {
        logger.error(ex);
        error(comp, ex.toString());
    }

    public static void error(Component comp, String message) {
        JOptionPane.showMessageDialog(comp, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm2(Component comp, String message) {
        int ans = JOptionPane.showConfirmDialog(
            comp, message, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        return ans == JOptionPane.YES_OPTION;
    }
}
