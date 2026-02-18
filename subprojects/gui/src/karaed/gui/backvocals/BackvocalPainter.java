package karaed.gui.backvocals;

import karaed.engine.formats.ranges.Range;
import karaed.gui.components.model.BackvocalRanges;
import karaed.gui.components.music.Measurer;
import karaed.gui.components.music.Painter;

import java.awt.Color;
import java.awt.Graphics;

final class BackvocalPainter extends Painter {

    private static final Color BV_COLOR = Color.green.darker();

    private final BackvocalSizer s;

    BackvocalPainter(Graphics g, BackvocalSizer s, int height) {
        super(g, s, height);
        this.s = s;
    }

    void paintBackvocals(BackvocalRanges ranges) {
        int ya = s.backEditY1();
        for (Range range : ranges.getRanges()) {
            int x1 = s.frame2x(range.from());
            int x2 = s.frame2x(range.to());
            int width = Measurer.width(x1, x2);
            g.setColor(Color.white);
            g.fillRect(x1, ya, width, BackvocalSizer.BV_EDIT_H);
            g.setColor(BV_COLOR);
            g.fillRect(x1, ya + 5, width, BackvocalSizer.BV_EDIT_H - 9);
            g.setColor(Color.darkGray);
            g.drawRect(x1, ya, width, BackvocalSizer.BV_EDIT_H);
        }
    }
}
