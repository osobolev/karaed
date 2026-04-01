package karaed.gui.util;

import karaed.gui.ScaleUIDefaults;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;

public final class SimpleGlassPane extends JPanel {

    private final JLabel lblWait = new JLabel();

    private SimpleGlassPane() {
        super(new BorderLayout());
        lblWait.setHorizontalAlignment(JLabel.CENTER);
        ScaleUIDefaults.resizeFont(lblWait, 16f);
        add(lblWait, BorderLayout.CENTER);
    }

    public static SimpleGlassPane create() {
        SimpleGlassPane glass = new SimpleGlassPane();
        glass.setOpaque(false);
        glass.addMouseListener(new MouseAdapter() {
        });
        glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        return glass;
    }

    public void setLabel(String label) {
        lblWait.setText(label);
    }

    public void show(String label) {
        setLabel(label);
        setVisible(true);
        requestFocusInWindow();
    }
}
