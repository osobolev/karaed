package karaed.gui;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

public final class ScaleUIDefaults {

    public static void resizeFont(JComponent comp, float size) {
        comp.setFont(comp.getFont().deriveFont(size(size)));
    }

    public static float size(float size) {
        return size * 1.25f;
    }

    public static int isize(int size) {
        return Math.round(size(size));
    }

    static void init() {
        UIDefaults defaults = UIManager.getDefaults();
        for (Object key : defaults.keySet()) {
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource font) {
                float newSize = size(font.getSize2D());
                FontUIResource newFont = new FontUIResource(font.deriveFont(newSize));
                UIManager.put(key, newFont);
            }
        }
    }
}
