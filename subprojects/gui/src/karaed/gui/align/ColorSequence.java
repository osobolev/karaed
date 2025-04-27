package karaed.gui.align;

import java.awt.Color;

public final class ColorSequence {

    private static final Color[] COLORS = {
        new Color(255, 150, 150),
        new Color(150, 255, 150),
        new Color(150, 150, 255),
        new Color(150, 255, 255),
        new Color(255, 150, 255),
        new Color(255, 255, 150)
    };
    private static final Color[] TRANSCLUCENT = new Color[COLORS.length];

    static {
        for (int i = 0; i < COLORS.length; i++) {
            Color c = COLORS[i];
            TRANSCLUCENT[i] = forText(c);
        }
    }

    private static float addTransparency(int value, float alpha) {
        float vf = value / 255f;
        return (vf + alpha - 1f) / (vf * alpha);
    }

    public static Color forText(Color c) {
        float alpha = 0.5f;
        return new Color(
            addTransparency(c.getRed(), alpha),
            addTransparency(c.getGreen(), alpha),
            addTransparency(c.getBlue(), alpha),
            alpha
        );
    }

    private int n = 0;

    void setNumber(int n) {
        this.n = n;
    }

    public Color getColor(int i, boolean opaque) {
        if (i >= n)
            return null;
        int index = i % COLORS.length;
        return (opaque ? COLORS : TRANSCLUCENT)[index];
    }
}
