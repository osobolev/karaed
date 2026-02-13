package karaed.engine.steps.video;

import karaed.engine.formats.backvocals.BackRange;

import java.util.ArrayList;
import java.util.List;

final class RangesToCommands {

    private final List<String> backVocals = new ArrayList<>();

    private void add(double ts, boolean on) {
        backVocals.add(String.format("%s volume@myvol volume %s;", ts, on ? "1.0" : "0.0"));
    }

    List<String> convert(List<BackRange> ranges) {
        double prevEnd = 0;
        for (BackRange range : ranges) {
            if (range.from() > prevEnd) {
                add(prevEnd, false);
            }
            add(range.from(), true);
            prevEnd = range.to();
        }
        if (prevEnd > 0) {
            add(prevEnd, false);
        }
        return backVocals;
    }
}
