package karaed.gui.components.model;

import karaed.engine.formats.ranges.RangeLike;

public final class EditableBackRange implements RangeLike {

    private final int from;
    private final int to;

    public EditableBackRange(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public int from() {
        return from;
    }

    @Override
    public int to() {
        return to;
    }
}
