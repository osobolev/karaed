package karaed.manual.gui;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;

final class InputUtil {

    public static JButton getChooseButtonFor(JComponent comp, String text, Runnable action) {
        JButton butt = new JButton(new AbstractAction(text) {
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        butt.setMargin(new Insets(0, 5, 0, 5));
        butt.setFocusPainted(false);
        if (comp != null) {
            Insets i1 = comp.getBorder() == null ? new Insets(5, 0, 5, 0) : comp.getBorder().getBorderInsets(comp);
            Insets i2 = butt.getBorder().getBorderInsets(butt);
            butt.setPreferredSize(new Dimension(
                butt.getPreferredSize().width,
                comp.getPreferredSize().height + i1.top + i1.bottom - i2.top - i2.bottom + 1
            ));
        }
        return butt;
    }
}
