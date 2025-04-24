package karaed.gui.align.model;

import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.RangeLike;

public final class EditableArea implements RangeLike {

    private final int from;
    private final int to;
    AreaParams params;

    EditableArea(int from, int to, AreaParams params) {
        this.from = from;
        this.to = to;
        this.params = params;
    }

    public int from() {
        return from;
    }

    public int to() {
        return to;
    }

    public AreaParams params() {
        return params;
    }
}
