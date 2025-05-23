package karaed.gui.align.vocals;

import karaed.engine.formats.ranges.Range;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;

import java.awt.FontMetrics;

class Sizer extends Measurer {

    static final int SEEK_H = 4;
    static final int RANGE_H = 20;
    static final int AREA_EDIT_H = 15;

    final FontMetrics fm;

    Sizer(FontMetrics fm, float frameRate, float pixPerSec) {
        super(frameRate, pixPerSec);
        this.fm = fm;
    }

    final int seekY1() {
        int h = fm.getHeight();
        return h + 13;
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

    final Range findRange(int frame, int y, EditableRanges model) {
        if (isRangeY(y)) {
            return model.findRange(frame);
        }
        return null;
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
