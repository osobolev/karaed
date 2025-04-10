package karaed.karagen.json.aligned;

import karaed.karagen.json.Shiftable;

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
