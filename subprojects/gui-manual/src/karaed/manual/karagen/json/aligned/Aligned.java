package karaed.manual.karagen.json.aligned;

import com.google.gson.annotations.SerializedName;
import karaed.manual.karagen.json.Shiftable;

import java.util.List;

public record Aligned(
    List<AlignSegment> segments,
    @SerializedName("word_segments")
    List<WordSegment> wordSegments
) implements Shiftable<Aligned> {

    @Override
    public Aligned shift(double shift) {
        return new Aligned(
            Shiftable.shiftList(segments, shift),
            Shiftable.shiftList(wordSegments, shift)
        );
    }
}
