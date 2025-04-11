package karaed.engine.formats.aligned;

import karaed.engine.formats.Shiftable;

import java.util.List;

public record AlignSegment(
    double start,
    double end,
    String text,
    List<WordSegment> words,
    List<CharSegment> chars
) implements Shiftable<AlignSegment> {

    @Override
    public AlignSegment shift(double shift) {
        return new AlignSegment(
            shift + start, shift + end,
            text,
            Shiftable.shiftList(words, shift),
            Shiftable.shiftList(chars, shift)
        );
    }
}
