package karaed.engine.opts;

public record OAlign(
    boolean words,
    boolean vocalsOnly
) {

    public OAlign() {
        this(true, true);
    }
}
