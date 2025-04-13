package karaed.engine.formats.aligned;

import karaed.engine.formats.Shiftable;

import java.util.List;

public record AlignSegment(
    Double start,
    Double end,
    String text,
    List<WordSegment> words,
    List<CharSegment> chars
) implements Shiftable<AlignSegment> {

    @Override
    public AlignSegment shift(double shift) {
        return new AlignSegment(
            Shiftable.shift(start, shift), Shiftable.shift(end, shift),
            text,
            Shiftable.shiftList(words, shift),
            Shiftable.shiftList(chars, shift)
        );
    }
}
