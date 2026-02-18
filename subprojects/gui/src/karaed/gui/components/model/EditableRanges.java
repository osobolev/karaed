package karaed.gui.components.model;

import karaed.engine.audio.PreparedAudioSource;
import karaed.engine.audio.RangeParams;
import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;

import java.util.*;

public final class EditableRanges {

    public final PreparedAudioSource source;

    private AreaParams params;
    private final RangeList<Range> ranges = new RangeList<>();
    private final RangeList<EditableArea> areas = new RangeList<>();

    private final List<RangeEditListener> listeners = new ArrayList<>();

    public EditableRanges(PreparedAudioSource source,
                          AreaParams params, List<Range> ranges, List<Area> areas) {
        this.source = source;
        this.params = params;
        replaceRanges(ranges);
        for (Area area : areas) {
            this.areas.add(new EditableArea(area.from(), area.to(), area.params()));
        }
    }

    private void replaceRanges(List<Range> ranges) {
        this.ranges.clear();
        this.ranges.addAll(ranges);
    }

    public void setRangesSilent(EditableArea area, AreaParams params, List<Range> ranges) {
        if (area == null) {
            this.params = params;
        } else {
            area.params = params;
        }
        replaceRanges(ranges);
    }

    private static boolean differs(Collection<Range> my, Collection<Range> other) {
        if (my.size() != other.size())
            return true;
        Iterator<Range> i1 = my.iterator();
        Iterator<Range> i2 = other.iterator();
        while (i1.hasNext()) {
            Range r1 = i1.next();
            Range r2 = i2.next();
            if (!r1.equals(r2))
                return true;
        }
        return true;
    }

    private void resplit(boolean fireNotChanged) {
        List<Range> ranges = source.detectVoice(getRangeParams());
        boolean changed = differs(ranges, this.ranges.values());
        if (changed) {
            replaceRanges(ranges);
            fireChanged(true);
        } else if (fireNotChanged) {
            fireChanged(false);
        }
    }

    public void splitByParams(EditableArea area, AreaParams params) {
        if (area == null) {
            this.params = params;
        } else {
            area.params = params;
        }
        resplit(false);
    }

    private RangeParams getRangeParams() {
        float frameRate = source.frameRate();
        int[] silenceThresholds = new int[source.frames()];
        Arrays.fill(silenceThresholds, params.silenceThreshold());
        for (EditableArea area : getAreas()) {
            Arrays.fill(silenceThresholds, area.from(), area.to(), area.params().silenceThreshold());
        }
        return new RangeParams() {

            @Override
            public int silenceThreshold(int frame) {
                return silenceThresholds[frame];
            }

            @Override
            public int maxSilenceGap(int frame) {
                EditableArea area = findArea(frame);
                AreaParams frameParams = area == null ? params : area.params();
                return (int) (frameParams.maxSilenceGap() * frameRate);
            }

            @Override
            public int minRangeDuration(int frame) {
                EditableArea area = findArea(frame);
                AreaParams frameParams = area == null ? params : area.params();
                return (int) (frameParams.minRangeDuration() * frameRate);
            }
        };
    }

    private void areasChanged() {
        resplit(true);
    }

    public void addArea(EditableArea area) {
        areas.add(area);
        areasChanged();
    }

    public void removeArea(EditableArea area) {
        if (areas.remove(area)) {
            areasChanged();
        }
    }

    public void resizeArea(EditableArea area, int from, int to) {
        if (!areas.remove(area))
            return;
        EditableArea newArea = new EditableArea(from, to, area.params());
        if (areas.intersects(newArea)) {
            areas.add(area);
            return;
        }
        addArea(newArea);
    }

    public EditableArea newArea(int from, int to) {
        EditableArea area = new EditableArea(from, to, params);
        if (areas.intersects(area))
            return null;
        return area;
    }

    public EditableArea newAreaFromRange(Range range, int delta) {
        int from;
        Range before = ranges.before(range);
        if (before != null) {
            int prev = before.to();
            from = Math.max(range.from() - delta, Range.mid(prev, range.from()));
        } else {
            from = Math.max(range.from() - delta, 0);
        }
        int to;
        Range after = ranges.after(range);
        if (after != null) {
            int next = after.from();
            to = Math.min(range.to() + delta, Range.mid(range.to(), next));
        } else {
            to = Math.min(range.to() + delta, source.frames());
        }
        int leftInset = range.from() - from;
        int rightInset = to - range.to();
        int inset = Math.min(leftInset, rightInset);
        EditableArea area = new EditableArea(range.from() - inset, range.to() + inset, params);
        if (areas.intersects(area))
            return null;
        return area;
    }

    public void addListener(RangeEditListener listener) {
        listeners.add(listener);
    }

    private void fireChanged(boolean rangesChanged) {
        for (RangeEditListener listener : listeners) {
            listener.changed(rangesChanged);
        }
    }

    public AreaParams getParams() {
        return params;
    }

    public int getRangeCount() {
        return ranges.size();
    }

    public Collection<Range> getRanges() {
        return ranges.values();
    }

    public Range findRange(int frame) {
        return ranges.findContaining(frame);
    }

    public int getAreaCount() {
        return areas.size();
    }

    public Collection<EditableArea> getAreas() {
        return areas.values();
    }

    public EditableArea findArea(int frame) {
        return areas.findContaining(frame);
    }

    public RangeSide isOnAreaBorder(int frame, int delta, EditableArea[] area) {
        return areas.isOnBorder(frame, delta, area);
    }
}
