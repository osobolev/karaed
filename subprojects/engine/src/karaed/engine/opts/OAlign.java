package karaed.engine.opts;

public record OAlign(
    boolean words
) {

    public OAlign() {
        this(false);
    }
}
