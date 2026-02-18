package karaed.gui.components.model;

import karaed.engine.audio.AudioSource;
import karaed.engine.formats.backvocals.Backvocals;
import karaed.engine.formats.ranges.Range;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// todo: move to backvocals package???
public final class BackvocalRanges {

    private final RangeList<Range> ranges = new RangeList<>();

    private final List<Runnable> listeners = new ArrayList<>();

    public BackvocalRanges(List<Range> ranges) {
        this.ranges.addAll(ranges);
    }

    private void rangesChanged() {
        fireChanged();
    }

    public void addRange(Range range) {
        ranges.add(range);
        rangesChanged();
    }

    public void removeRange(Range range) {
        if (ranges.remove(range)) {
            rangesChanged();
        }
    }

    public void resizeRange(Range range, int from, int to) {
        if (!ranges.remove(range))
            return;
        Range newRange = new Range(from, to);
        if (ranges.intersects(newRange)) {
            ranges.add(range);
            return;
        }
        addRange(newRange);
    }

    public Range newRange(int from, int to) {
        Range range = new Range(from, to);
        if (ranges.intersects(range))
            return null;
        return range;
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void fireChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public Collection<Range> getRanges() {
        return ranges.values();
    }

    public Range findRange(int frame) {
        return ranges.findContaining(frame);
    }

    public RangeSide isOnRangeBorder(int frame, int delta, Range[] range) {
        return ranges.isOnBorder(frame, delta, range);
    }

    private static int sec2frame(double sec, float frameRate) {
        return AudioSource.sec2frame((float) sec, frameRate);
    }

    public static BackvocalRanges convert(Backvocals bv, float frameRate) {
        List<Range> ranges = bv
            .ranges()
            .stream()
            .map(r -> new Range(sec2frame(r.from(), frameRate), sec2frame(r.to(), frameRate)))
            .toList();
        return new BackvocalRanges(ranges);
    }
}
