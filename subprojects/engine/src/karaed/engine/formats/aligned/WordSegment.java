package karaed.engine.formats.aligned;

import karaed.engine.formats.Shiftable;

public record WordSegment(
    double start,
    double end,
    double score,
    String word
) implements Shiftable<WordSegment> {

    @Override
    public WordSegment shift(double shift) {
        return new WordSegment(shift + start, shift + end, score, word);
    }
}
