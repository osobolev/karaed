package karaed.gui.backvocals;

import karaed.gui.components.model.BackvocalRanges;
import karaed.gui.components.model.EditableBackRange;
import karaed.gui.components.music.Measurer;
import karaed.gui.components.music.Painter;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

final class BackvocalPainter extends Painter {

    private static final Color BV_COLOR = Color.green.darker();

    private final BackvocalSizer s;

    BackvocalPainter(Graphics g, BackvocalSizer s, int height) {
        super(g, s, height);
        this.s = s;
    }

    static int toPercent(Double coeff) {
        return (int) Math.round(coeff.doubleValue() * 100.0);
    }

    void paintBackvocals(BackvocalRanges ranges) {
        int ya = s.backEditY1();
        FontMetrics fm = s.fm;
        int texty = (BackvocalSizer.BV_EDIT_H - fm.getHeight()) / 2 + fm.getAscent();
        for (EditableBackRange range : ranges.getRanges()) {
            int x1 = s.frame2x(range.from());
            int x2 = s.frame2x(range.to());
            int width = Measurer.width(x1, x2);
            g.setColor(Color.white);
            g.fillRect(x1, ya, width, BackvocalSizer.BV_EDIT_H);
            g.setColor(BV_COLOR);
            g.fillRect(x1, ya + 5, width, BackvocalSizer.BV_EDIT_H - 9);

            Double coeff = range.getCoeff();
            if (coeff != null) {
                String str = toPercent(coeff) + "%";
                int tw = fm.stringWidth(str);
                int tx = x1 + (width - tw) / 2;
                g.setColor(Color.white);
                g.fillRect(tx - 5, ya, tw + 8, BackvocalSizer.BV_EDIT_H);
                g.setColor(Color.black);
                g.drawString(str, tx, ya + texty);
            }

            g.setColor(Color.darkGray);
            g.drawRect(x1, ya, width, BackvocalSizer.BV_EDIT_H);
        }
    }
}
