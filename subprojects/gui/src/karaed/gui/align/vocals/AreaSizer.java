package karaed.gui.align.vocals;

import karaed.gui.components.model.EditableArea;
import karaed.gui.components.model.EditableRanges;
import karaed.gui.components.music.Sizer;

import java.awt.FontMetrics;

final class AreaSizer extends Sizer {

    static final int AREA_EDIT_H = 15;

    AreaSizer(FontMetrics fm, float frameRate, float pixPerSec) {
        super(fm, frameRate, pixPerSec);
    }

    int areaEditY1() {
        return rangeY1() + RANGE_H + 15;
    }

    protected int prefHeight() {
        return areaEditY1() + AREA_EDIT_H + 10;
    }

    boolean isAreaEditY(int y) {
        int delta = y - areaEditY1();
        return delta >= 0 && delta < AREA_EDIT_H;
    }

    EditableArea findArea(int frame, int y, EditableRanges model) {
        if (isAreaEditY(y)) {
            return model.findArea(frame);
        }
        return null;
    }
}
