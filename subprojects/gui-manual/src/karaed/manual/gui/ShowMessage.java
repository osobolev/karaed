package karaed.manual.gui;

import javax.swing.*;
import java.awt.Component;

final class ShowMessage {

    static void error(Component comp, Throwable ex) {
        error(comp, ex.toString());
    }

    static void error(Component comp, String message) {
        JOptionPane.showMessageDialog(comp, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static Boolean confirm3(Component comp, String message) {
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
