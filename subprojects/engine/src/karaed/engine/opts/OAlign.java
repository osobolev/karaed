package karaed.engine.opts;

public record OAlign(
    boolean words,
    double tagPause
) {

    public OAlign() {
        this(true, 0.5);
    }
}
