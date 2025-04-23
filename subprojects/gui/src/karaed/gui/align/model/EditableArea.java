package karaed.gui.align.model;

import karaed.engine.formats.ranges.AreaParams;

public final class EditableArea {

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

    boolean contains(int frame) {
        return from <= frame && frame < to;
    }
}
