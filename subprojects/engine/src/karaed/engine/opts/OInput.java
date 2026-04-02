package karaed.engine.opts;

public record OInput(
    String url,
    String file,
    Boolean videoFile
) {

    public OInput() {
        this(null, null, null);
    }

    public boolean hasVideo() {
        if (url != null)
            return true;
        return videoFile != null && videoFile.booleanValue();
    }
}
