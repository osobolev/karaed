package karaed.gui.components.music;

import karaed.engine.formats.ranges.Range;
import karaed.gui.components.model.EditableRanges;

import java.awt.FontMetrics;

public abstract class Sizer extends Measurer {

    static final int SEEK_H = 4;
    public static final int RANGE_H = 20;

    final FontMetrics fm;

    protected Sizer(FontMetrics fm, float frameRate, float pixPerSec) {
        super(frameRate, pixPerSec);
        this.fm = fm;
    }

    final int seekY1() {
        int h = fm.getHeight();
        return h + 13;
    }

    protected final int rangeY1() {
        int h = fm.getHeight();
        return h + 20;
    }

    protected abstract int prefHeight();

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
}
