package karaed.gui.options;

import karaed.gui.util.BaseWindow;

import javax.swing.*;
import java.awt.Component;

final class ValidationException extends Exception {

    final JComponent component;

    ValidationException(String message, JComponent component) {
        super(message);
        this.component = component;
    }

    void show(BaseWindow owner) {
        openTabContaining(component);
        component.requestFocusInWindow();
        owner.error(getMessage());
    }

    private static void openTabContaining(JComponent comp) {
        if (comp.isShowing())
            return;
        Component current = comp;
        while (current != null) {
            if (current instanceof JTabbedPane tabs) {
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Component tab = tabs.getComponentAt(i);
                    if (SwingUtilities.isDescendingFrom(comp, tab)) {
                        tabs.setSelectedIndex(i);
                        break;
                    }
                }
            }
            current = current.getParent();
        }
    }
}
