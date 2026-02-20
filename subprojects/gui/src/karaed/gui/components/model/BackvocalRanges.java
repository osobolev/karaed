package karaed.gui.components.model;

import karaed.engine.audio.AudioSource;
import karaed.engine.formats.backvocals.BackRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class BackvocalRanges {

    private final RangeList<EditableBackRange> ranges = new RangeList<>();

    private final List<Runnable> listeners = new ArrayList<>();

    public BackvocalRanges(List<EditableBackRange> ranges) {
        this.ranges.addAll(ranges);
    }

    private void rangesChanged() {
        fireChanged();
    }

    public void addRange(EditableBackRange range) {
        ranges.add(range);
        rangesChanged();
    }

    public void removeRange(EditableBackRange range) {
        if (ranges.remove(range)) {
            rangesChanged();
        }
    }

    public void resizeRange(EditableBackRange range, int from, int to) {
        if (!ranges.remove(range))
            return;
        EditableBackRange newRange = new EditableBackRange(from, to);
        if (ranges.intersects(newRange)) {
            ranges.add(range);
            return;
        }
        addRange(newRange);
    }

    public EditableBackRange newRange(int from, int to) {
        EditableBackRange range = new EditableBackRange(from, to);
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

    public Collection<EditableBackRange> getRanges() {
        return ranges.values();
    }

    public EditableBackRange findRange(int frame) {
        return ranges.findContaining(frame);
    }

    public RangeSide isOnRangeBorder(int frame, int delta, EditableBackRange[] range) {
        return ranges.isOnBorder(frame, delta, range);
    }

    private static int sec2frame(double sec, float frameRate) {
        return AudioSource.sec2frame((float) sec, frameRate);
    }

    public static BackvocalRanges convert(List<BackRange> backRanges, float frameRate) {
        List<EditableBackRange> ranges = backRanges
            .stream()
            .map(r -> new EditableBackRange(sec2frame(r.from(), frameRate), sec2frame(r.to(), frameRate)))
            .toList();
        return new BackvocalRanges(ranges);
    }
}
