package karaed.gui.components.model;

import karaed.engine.formats.ranges.RangeLike;

public final class EditableBackRange implements RangeLike {

    private final int from;
    private final int to;
    Double coeff;

    public EditableBackRange(int from, int to, Double coeff) {
        this.from = from;
        this.to = to;
        this.coeff = coeff;
    }

    @Override
    public int from() {
        return from;
    }

    @Override
    public int to() {
        return to;
    }

    public Double getCoeff() {
        return coeff;
    }
}
