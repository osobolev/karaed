package karaed.engine.formats.aligned;

import com.google.gson.annotations.SerializedName;
import karaed.engine.formats.Shiftable;

public record CharSegment(
    Double start,
    Double end,
    Double score,
    @SerializedName("char")
    String ch
) implements Shiftable<CharSegment> {

    @Override
    public CharSegment shift(double shift) {
        return new CharSegment(
            Shiftable.shift(start, shift), Shiftable.shift(end, shift),
            score, ch
        );
    }

    public char getChar() {
        return ch.length() > 0 ? ch.charAt(0) : ' ';
    }
}
