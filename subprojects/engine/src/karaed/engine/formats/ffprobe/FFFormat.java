package karaed.engine.formats.ffprobe;

public record FFFormat(
    String duration,
    FFTags tags
) {

    public double parsedDuration() {
        return Double.parseDouble(duration);
    }
}
