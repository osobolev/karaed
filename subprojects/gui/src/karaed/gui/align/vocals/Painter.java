package karaed.gui.align.vocals;

import karaed.engine.formats.ranges.Range;
import karaed.gui.align.ColorSequence;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

final class Painter extends Sizer {

    private static final int[] SECOND_TICKS = {1, 5, 10, 30, 60};

    private final Graphics g;
    private final int height;

    Painter(Graphics g, FontMetrics fm, float frameRate, float pixPerSec, int height) {
        super(fm, frameRate, pixPerSec);
        this.g = g;
        this.height = height;
    }

    void paintPlay(Range range, long millis) {
        int x1 = frame2x(range.from());
        int x2 = frame2x(range.to());
        int y0 = seekY1();
        int h = SEEK_H;
        {
            g.setColor(Color.black);
            int width = width(x1, x2);
            g.fillRect(x1, y0, width, h);
        }
        {
            int position = sec2pix(millis / 1000f);
            g.setColor(Color.white);
            g.fillRect(x1 + position - 2, y0, 4, h);
        }
    }

    private int getTick(int minTickWidth, int[] ticks) {
        for (int tickSize : ticks) {
            int pixels = sec2pix(tickSize);
            if (pixels >= minTickWidth) {
                return tickSize;
            }
        }
        return 0;
    }

    void paintScale(int seconds, int width) {
        int h = fm.getHeight();
        g.setColor(Color.black);
        g.drawLine(0, h, width, h);

        int bigTick = getTick(fm.stringWidth("00:00") * 4, SECOND_TICKS);
        if (bigTick > 0) {
            for (int s = 0; s <= seconds; s += bigTick) {
                int x = sec2x(s);
                String str = Range.formatTime(s);
                g.drawString(str, x - fm.stringWidth(str) / 2, fm.getAscent());
                g.drawLine(x, h, x, h + 10);
            }
        }
        int smallTick = getTick(10, SECOND_TICKS);
        if (smallTick > 0) {
            for (int s = 0; s <= seconds; s += smallTick) {
                int x = sec2x(s);
                g.drawLine(x, h, x, h + 5);
            }
        }
    }

    void paint(ColorSequence colors, EditableRanges model, EditableArea editingArea,
               RangeIndexes rangeIndexes) {
        int ya = areaEditY1();
        for (EditableArea area : model.getAreas()) {
            int x1 = frame2x(area.from());
            int x2 = frame2x(area.to());
            int width = width(x1, x2);
            if (area != editingArea) {
                g.setColor(new Color(120, 120, 120, 120)); // todo
                g.fillRect(x1, 0, width, height); // todo
            }
            g.setColor(Color.red); // todo!!!
            g.fillRect(x1, ya, width, AREA_EDIT_H);
        }

        int yr = rangeY1();
        int i = 0;
        for (Range range : model.getRanges()) {
            int colorIndex = i++;
            if (rangeIndexes != null) {
                rangeIndexes.add(colorIndex, range);
            }
            Color color = colors.getColor(colorIndex, true);
            g.setColor(color == null ? Color.black : color);
            int x1 = frame2x(range.from());
            int x2 = frame2x(range.to());
            int width = width(x1, x2);
            g.fillRect(x1, yr, width, RANGE_H);
        }
    }

    void paintDrag(int x) {
        g.setColor(Color.black);
        g.drawLine(x, 0, x, height);
    }
}
