package karaed.gui.align;

import karaed.engine.formats.ranges.Range;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;

import java.awt.FontMetrics;
import java.util.List;

class Sizer extends Measurer {

    static final int RANGE_H = 20;
    static final int AREA_EDIT_H = 15;

    final FontMetrics fm;

    Sizer(FontMetrics fm, float frameRate, float pixPerSec) {
        super(frameRate, pixPerSec);
        this.fm = fm;
    }

    final int rangeY1() {
        int h = fm.getHeight();
        return h + 20;
    }

    final int areaEditY1() {
        return rangeY1() + RANGE_H + 15;
    }

    final int prefHeight() {
        return areaEditY1() + AREA_EDIT_H + 10;
    }

    final boolean isRangeY(int y) {
        int delta = y - rangeY1();
        return delta >= 0 && delta < RANGE_H;
    }

    final int findRange(int frame, int y, List<Range> ranges) {
        if (isRangeY(y)) {
            for (int i = 0; i < ranges.size(); i++) {
                Range range = ranges.get(i);
                if (frame >= range.from() && frame < range.to()) {
                    return i;
                }
            }
        }
        return -1;
    }

    final boolean isAreaEditY(int y) {
        int delta = y - areaEditY1();
        return delta >= 0 && delta < AREA_EDIT_H;
    }

    final EditableArea findArea(int frame, int y, EditableRanges model) {
        if (isAreaEditY(y)) {
            return model.findArea(frame);
        }
        return null;
    }
}
