package karaed.engine.opts;

public record OInput(
    String url,
    String file
) {

    public OInput() {
        this(null, null);
    }
}
