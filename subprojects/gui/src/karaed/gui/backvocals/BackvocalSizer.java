package karaed.gui.backvocals;

import karaed.engine.formats.ranges.Range;
import karaed.gui.components.model.BackvocalRanges;
import karaed.gui.components.music.Sizer;

import java.awt.FontMetrics;

final class BackvocalSizer extends Sizer {

    static final int BV_EDIT_H = 20;

    BackvocalSizer(FontMetrics fm, float frameRate, float pixPerSec) {
        super(fm, frameRate, pixPerSec);
    }

    int backSeekY1() {
        return backEditY1() - 7;
    }

    int backEditY1() {
        return rangeY1() + RANGE_H + 20;
    }

    @Override
    protected int prefHeight() {
        return backEditY1() + BV_EDIT_H + 15;
    }

    boolean isBackEditY(int y) {
        int delta = y - backEditY1();
        return delta >= 0 && delta < BV_EDIT_H;
    }

    Range findBackRange(int frame, int y, BackvocalRanges ranges) {
        if (isBackEditY(y)) {
            return ranges.findRange(frame);
        }
        return null;
    }
}
