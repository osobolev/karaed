package karaed.engine.opts;

public record OAlign(
    boolean words,
    double tagPause
) {

    public OAlign() {
        this(false, 0.5);
    }
}
