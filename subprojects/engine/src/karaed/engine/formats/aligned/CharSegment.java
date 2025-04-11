package karaed.engine.formats.aligned;

import com.google.gson.annotations.SerializedName;
import karaed.engine.formats.Shiftable;

public record CharSegment(
    double start,
    double end,
    double score,
    @SerializedName("char")
    String ch
) implements Shiftable<CharSegment> {

    @Override
    public CharSegment shift(double shift) {
        return new CharSegment(shift + start, shift + end, score, ch);
    }
}
