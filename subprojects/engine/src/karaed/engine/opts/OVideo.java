package karaed.engine.opts;

public record OVideo(
    boolean useOriginalVideo
) {

    public OVideo() {
        this(true);
    }
}
