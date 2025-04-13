package karaed.engine.formats.aligned;

import karaed.engine.formats.Shiftable;

public record WordSegment(
    Double start,
    Double end,
    Double score,
    String word
) implements Shiftable<WordSegment> {

    @Override
    public WordSegment shift(double shift) {
        return new WordSegment(
            Shiftable.shift(start, shift), Shiftable.shift(end, shift),
            score, word
        );
    }
}
