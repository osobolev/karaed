package karaed.gui.align.model;

import karaed.engine.audio.PreparedAudioSource;
import karaed.engine.audio.RangeParams;
import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class EditableRanges {

    public final PreparedAudioSource source;

    private AreaParams params;
    private final List<Range> ranges;
    private final RangeList<EditableArea> areas = new RangeList<>();

    private final List<RangeEditListener> listeners = new ArrayList<>();

    public EditableRanges(PreparedAudioSource source,
                          AreaParams params, List<Range> ranges, List<Area> areas) {
        this.source = source;
        this.params = params;
        this.ranges = new ArrayList<>(ranges);
        for (Area area : areas) {
            this.areas.add(new EditableArea(area.from(), area.to(), area.params()));
        }
    }

    public void setRangesSilent(EditableArea area, AreaParams params, List<Range> ranges) {
        if (area == null) {
            this.params = params;
        } else {
            area.params = params;
        }
        this.ranges.clear();
        this.ranges.addAll(ranges);
    }

    private void resplit(boolean fireNotChanged) {
        List<Range> ranges = source.detectVoice(getRangeParams());
        boolean changed = !this.ranges.equals(ranges);
        if (changed) {
            this.ranges.clear();
            this.ranges.addAll(ranges);
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
        int i = ranges.indexOf(range);
        if (i < 0)
            return null;
        int from;
        if (i > 0) {
            int prev = ranges.get(i - 1).to();
            from = Math.max(range.from() - delta, Range.mid(prev, range.from()));
        } else {
            from = Math.max(range.from() - delta, 0);
        }
        int to;
        if (i + 1 < ranges.size()) {
            int next = ranges.get(i + 1).from();
            to = Math.min(range.to() + delta, Range.mid(range.to(), next));
        } else {
            to = Math.min(range.to() + delta, source.frames());
        }
        EditableArea area = new EditableArea(from, to, params);
        if (areas.intersects(area))
            return null;
        return area;
    }

    public void addListener(RangeEditListener listener) {
        listeners.add(listener);
    }

    private void fireChanged(boolean rangesChanged) {
        for (RangeEditListener runnable : listeners) {
            runnable.changed(rangesChanged);
        }
    }

    public AreaParams getParams() {
        return params;
    }

    public int getRangeCount() {
        return ranges.size();
    }

    public Collection<Range> getRanges() {
        return ranges;
    }

    public Range findRange(int frame) {
        for (Range range : ranges) {
            if (frame >= range.from() && frame < range.to()) {
                return range;
            }
        }
        return null;
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

    public AreaSide isOnAreaBorder(int frame, int delta, EditableArea[] area) {
        return areas.isOnBorder(frame, delta, area);
    }
}
