package karaed.gui.util;

import javax.swing.*;
import java.awt.Component;

public final class ShowMessage {

    public static void error(Component comp, String message) {
        JOptionPane.showMessageDialog(comp, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm2(Component comp, String message) {
        int ans = JOptionPane.showConfirmDialog(
            comp, message, "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE
        );
        return ans == JOptionPane.YES_OPTION;
    }

    public static Boolean confirm3(Component comp, String message) {
        int ans = JOptionPane.showConfirmDialog(
            comp, message, "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
        );
        if (ans == JOptionPane.YES_OPTION) {
            return true;
        } else if (ans == JOptionPane.NO_OPTION) {
            return false;
        } else {
            return null;
        }
    }
}
