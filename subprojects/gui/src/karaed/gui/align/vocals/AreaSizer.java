package karaed.gui.align.vocals;

import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;

import java.awt.FontMetrics;

class AreaSizer extends Sizer {

    static final int AREA_EDIT_H = 15;

    AreaSizer(FontMetrics fm, float frameRate, float pixPerSec) {
        super(fm, frameRate, pixPerSec);
    }

    final int areaEditY1() {
        return rangeY1() + RANGE_H + 15;
    }

    final int prefHeight() {
        return areaEditY1() + AREA_EDIT_H + 10;
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
