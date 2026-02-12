package karaed.gui.align.vocals;

import karaed.engine.formats.ranges.Range;
import karaed.gui.align.ColorSequence;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

final class AreaPainter {

    private static final int[] SECOND_TICKS = {1, 5, 10, 30, 60};

    /**
     * Background color for area
     */
    private static final Color AREA_BG = new Color(120, 120, 120, 120);
    /**
     * Background color for area action band
     */
    private static final Color ACTION_BAND_BG = Color.gray;

    private final Graphics g;
    private final Sizer s;
    private final int height;
    private final EditableRanges model;
    private final EditableArea editingArea;

    AreaPainter(Graphics g, Sizer s, int height,
                EditableRanges model, EditableArea editingArea) {
        this.g = g;
        this.s = s;
        this.height = height;
        this.model = model;
        this.editingArea = editingArea;
    }

    void paintPlay(Range range, long millis) {
        int x1 = s.frame2x(range.from());
        int x2 = s.frame2x(range.to());
        int y0 = s.seekY1();
        int h = Sizer.SEEK_H;
        {
            g.setColor(Color.black);
            int width = Measurer.width(x1, x2);
            g.fillRect(x1, y0, width, h);
        }
        int xp = x1 + s.sec2pix(millis / 1000f);
        if (xp < x2) {
            g.setColor(Color.white);
            g.fillRect(xp - 2, y0, 4, h);
            g.setColor(Color.gray);
            g.drawRect(xp - 3, y0 - 1, 5, h + 1);
        }
    }

    private int getTick(int minTickWidth, int[] ticks) {
        for (int tickSize : ticks) {
            int pixels = s.sec2pix(tickSize);
            if (pixels >= minTickWidth) {
                return tickSize;
            }
        }
        return 0;
    }

    void paintScale(int seconds, int width) {
        FontMetrics fm = s.fm;
        int h = fm.getHeight();
        g.setColor(Color.black);
        g.drawLine(0, h, width, h);

        int bigTick = getTick(fm.stringWidth("00:00") * 4, SECOND_TICKS);
        if (bigTick > 0) {
            for (int sec = 0; sec <= seconds; sec += bigTick) {
                int x = s.sec2x(sec);
                String str = Range.formatTime(sec);
                g.drawString(str, x - fm.stringWidth(str) / 2, fm.getAscent());
                g.drawLine(x, h, x, h + 10);
            }
        }
        int smallTick = getTick(10, SECOND_TICKS);
        if (smallTick > 0) {
            for (int sec = 0; sec <= seconds; sec += smallTick) {
                int x = s.sec2x(sec);
                g.drawLine(x, h, x, h + 5);
            }
        }
    }

    void paint(ColorSequence colors, RangeIndexes rangeIndexes) {
        int ya = s.areaEditY1();
        for (EditableArea area : model.getAreas()) {
            int x1 = s.frame2x(area.from());
            int x2 = s.frame2x(area.to());
            int width = Measurer.width(x1, x2);
            if (area != editingArea) {
                g.setColor(AREA_BG);
                g.fillRect(x1, 0, width, height);
            }
            g.setColor(ACTION_BAND_BG);
            g.fillRect(x1, ya, width, Sizer.AREA_EDIT_H);
            g.setColor(Color.darkGray);
            g.drawLine(x1, ya, x1 + width, ya);
            g.drawLine(x1, ya + Sizer.AREA_EDIT_H, x1 + width, ya + Sizer.AREA_EDIT_H);
        }

        int yr = s.rangeY1();
        int i = 0;
        for (Range range : model.getRanges()) {
            int colorIndex = i++;
            if (rangeIndexes != null) {
                rangeIndexes.add(colorIndex, range);
            }
            Color color = colors.getColor(colorIndex, true);
            g.setColor(color == null ? Color.black : color);
            int x1 = s.frame2x(range.from());
            int x2 = s.frame2x(range.to());
            int width = Measurer.width(x1, x2);
            g.fillRect(x1, yr, width, Sizer.RANGE_H);
        }
    }

    void paintDrag(int x) {
        g.setColor(Color.black);
        g.drawLine(x, 0, x, height);
    }
}
