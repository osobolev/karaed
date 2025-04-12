package karaed.gui.align;

import java.awt.Color;

final class ColorSequence {

    private static final Color[] COLORS = {
        new Color(255, 150, 150),
        new Color(150, 255, 150),
        new Color(150, 150, 255),
        new Color(150, 255, 255),
        new Color(255, 150, 255),
        new Color(255, 255, 150)
    };

    private int n = 0;

    void setNumber(int n) {
        this.n = n;
    }

    Color getColor(int i) {
        if (i >= n)
            return null;
        return COLORS[i % COLORS.length];
    }
}
