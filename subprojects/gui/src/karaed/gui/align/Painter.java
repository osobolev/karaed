package karaed.gui.align;

import karaed.engine.formats.ranges.Range;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Map;

final class Painter extends Sizer {

    private static final int[] SECOND_TICKS = {1, 5, 10, 30, 60};

    private final Graphics g;
    private final int height;

    Painter(Graphics g, FontMetrics fm, float frameRate, float pixPerSec, int height) {
        super(fm, frameRate, pixPerSec);
        this.g = g;
        this.height = height;
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

    void paint(ColorSequence colors, EditableRanges model, Range playingRange, EditableArea editingArea,
               Map<Range, Integer> rangeIndexes) {
        int ya = areaEditY1();
        for (EditableArea area : model.getAreas()) {
            int x1 = frame2x(area.from());
            int x2 = frame2x(area.to());
            int width = Math.max(x2 - x1, 1);
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
                rangeIndexes.put(range, colorIndex);
            }
            Color color = colors.getColor(colorIndex, true);
            g.setColor(color == null ? Color.black : color);
            int x1 = frame2x(range.from());
            int x2 = frame2x(range.to());
            int width = Math.max(x2 - x1, 1);
            g.fillRect(x1, yr, width, RANGE_H);
            if (range == playingRange) {
                g.setColor(Color.red);
                g.drawRect(x1 - 1, yr - 1, width + 1, RANGE_H + 1);
            }
        }
    }

    void paintDrag(int x) {
        g.setColor(Color.black);
        g.drawLine(x, 0, x, height);
    }
}
