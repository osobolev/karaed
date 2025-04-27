package karaed.gui.align.vocals;

import karaed.engine.formats.ranges.Range;

import java.util.HashMap;
import java.util.Map;

final class RangeIndexes {

    private final Map<Range, Integer> r2i = new HashMap<>();
    private final Map<Integer, Range> i2r = new HashMap<>();

    void clear() {
        r2i.clear();
        i2r.clear();
    }

    void add(int index, Range range) {
        r2i.put(range, index);
        i2r.put(index, range);
    }

    Integer getIndex(Range range) {
        return r2i.get(range);
    }

    Range getRange(int index) {
        return i2r.get(index);
    }
}
