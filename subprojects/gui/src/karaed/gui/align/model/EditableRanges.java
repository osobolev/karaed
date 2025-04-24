package karaed.gui.align.model;

import karaed.engine.audio.PreparedAudioSource;
import karaed.engine.audio.RangeParams;
import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;

import java.util.*;

public final class EditableRanges {

    public final PreparedAudioSource source;

    private AreaParams params;
    private final List<Range> ranges;
    private final TreeMap<Integer, EditableArea> areas = new TreeMap<>();

    private final List<RangeEditListener> listeners = new ArrayList<>();

    public EditableRanges(PreparedAudioSource source,
                          AreaParams params, List<Range> ranges, List<Area> areas) {
        this.source = source;
        this.params = params;
        this.ranges = new ArrayList<>(ranges);
        for (Area area : areas) {
            this.areas.put(area.from(), new EditableArea(area.from(), area.to(), area.params()));
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

    private boolean intersects(EditableArea newArea) {
        NavigableSet<Integer> keySet = areas.navigableKeySet();
        Integer before = keySet.floor(newArea.from()); // <= from
        if (before != null) {
            EditableArea areaBefore = areas.get(before);
            if (areaBefore.contains(newArea.from()))
                return true;
        }
        Integer after = keySet.higher(newArea.from()); // > from
        if (after != null) {
            EditableArea areaAfter = areas.get(after);
            if (newArea.contains(areaAfter.from()))
                return true;
        }
        return false;
    }

    private void areasChanged() {
        resplit(true);
    }

    public void addArea(EditableArea area) {
        if (intersects(area))
            return;
        areas.put(area.from(), area);
        areasChanged();
    }

    public void removeArea(EditableArea area) {
        if (areas.entrySet().removeIf(e -> e.getValue() == area)) {
            areasChanged();
        }
    }

    public void resizeArea(EditableArea area, int from, int to) {
        if (areas.remove(area.from()) != area)
            return;
        EditableArea newArea = new EditableArea(from, to, area.params());
        if (intersects(newArea)) {
            areas.put(area.from(), area);
            return;
        }
        addArea(newArea);
    }

    public EditableArea newArea(int from, int to) {
        EditableArea area = new EditableArea(from, to, params);
        if (intersects(area))
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
        if (intersects(area))
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
        Integer floor = areas.navigableKeySet().floor(frame);
        if (floor == null)
            return null;
        EditableArea area = areas.get(floor);
        if (area.contains(frame))
            return area;
        return null;
    }

    public AreaSide isOnAreaBorder(int frame, int delta, EditableArea[] area) {
        NavigableSet<Integer> keySet = areas.navigableKeySet();
        EditableArea areaBefore;
        int dx1;
        int dx2;
        Integer before = keySet.floor(frame); // <= frame
        if (before != null) {
            areaBefore = areas.get(before);
            dx1 = Math.abs(before.intValue() - frame);
            dx2 = Math.abs(areaBefore.to() - frame);
        } else {
            areaBefore = null;
            dx1 = Integer.MAX_VALUE;
            dx2 = Integer.MAX_VALUE;
        }
        EditableArea areaAfter;
        int dx3;
        Integer after = keySet.higher(frame); // > frame
        if (after != null) {
            areaAfter = areas.get(after);
            dx3 = Math.abs(after.intValue() - frame);
        } else {
            areaAfter = null;
            dx3 = Integer.MAX_VALUE;
        }
        if (dx1 <= dx2 && dx1 <= dx3) {
            if (dx1 < delta) {
                if (area != null) {
                    area[0] = areaBefore;
                }
                return AreaSide.LEFT;
            }
        } else if (dx2 <= dx1 && dx2 <= dx3) {
            if (dx2 < delta) {
                if (area != null) {
                    area[0] = areaBefore;
                }
                return AreaSide.RIGHT;
            }
        } else {
            if (dx3 < delta) {
                if (area != null) {
                    area[0] = areaAfter;
                }
                return AreaSide.LEFT;
            }
        }
        return null;
    }
}
