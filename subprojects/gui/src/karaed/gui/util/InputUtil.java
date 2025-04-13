package karaed.gui.util;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

public final class InputUtil {

    public static Icon getIcon(String path) {
        URL url = InputUtil.class.getResource(path);
        assert url != null;
        return new ImageIcon(url);
    }

    public static void setText(JTextComponent tc, String text) {
        tc.setText(text);
        tc.setCaretPosition(0);
    }

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

    public static File chooseFile(Component parent, FileFilter... filters) {
        JFileChooser chooser = new JFileChooser();
        for (FileFilter filter : filters) {
            chooser.addChoosableFileFilter(filter);
        }
        if (filters.length > 0) {
            chooser.setFileFilter(filters[0]);
        }
        int ans = chooser.showOpenDialog(parent);
        if (ans != JFileChooser.APPROVE_OPTION)
            return null;
        return chooser.getSelectedFile();
    }
}
