package karaed.engine.steps.video;

import karaed.engine.formats.backvocals.BackRange;

import java.util.ArrayList;
import java.util.List;

final class RangesToCommands {

    private final List<String> backVocals = new ArrayList<>();

    private void add(double ts, double coeff) {
        backVocals.add(String.format("%s volume@myvol volume %s;", ts, coeff));
    }

    List<String> convert(List<BackRange> ranges) {
        double prevEnd = 0;
        for (BackRange range : ranges) {
            if (range.from() > prevEnd) {
                add(prevEnd, 0.0);
            }
            add(range.from(), range.coefficient());
            prevEnd = range.to();
        }
        if (prevEnd > 0) {
            add(prevEnd, 0.0);
        }
        return backVocals;
    }
}
