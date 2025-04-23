package karaed.gui.align.model;

import karaed.engine.audio.MaxAudioSource;
import karaed.engine.audio.RangeParams;
import karaed.engine.audio.VoiceRanges;
import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.*;

public final class EditableRanges {

    public final MaxAudioSource source;

    private AreaParams params;
    private final List<Range> ranges;
    private final TreeMap<Integer, EditableArea> areas = new TreeMap<>();

    private final List<RangeEditListener> listeners = new ArrayList<>();

    public EditableRanges(MaxAudioSource source,
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

    private void resplit(boolean fireNotChanged) throws UnsupportedAudioFileException, IOException {
        List<Range> ranges = VoiceRanges.detectVoice(source, getRangeParams());
        boolean changed = !this.ranges.equals(ranges);
        if (changed) {
            this.ranges.clear();
            this.ranges.addAll(ranges);
            fireChanged(true);
        } else if (fireNotChanged) {
            fireChanged(false);
        }
    }

    public void splitByParams(EditableArea area, AreaParams params) throws UnsupportedAudioFileException, IOException {
        if (area == null) {
            this.params = params;
        } else {
            area.params = params;
        }
        resplit(false);
    }

    private RangeParams getRangeParams() {
        float frameRate = source.format.getFrameRate();
        float[] silenceThresholds = new float[source.frames];
        Arrays.fill(silenceThresholds, params.silenceThreshold());
        for (EditableArea area : getAreas()) {
            Arrays.fill(silenceThresholds, area.from(), area.to(), area.params().silenceThreshold());
        }
        return new RangeParams() {

            @Override
            public float silenceThreshold(int frame) {
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

    private void areasChanged() throws UnsupportedAudioFileException, IOException {
        resplit(true);
    }

    public void addArea(EditableArea area) throws UnsupportedAudioFileException, IOException {
        if (intersects(area))
            return;
        areas.put(area.from(), area);
        areasChanged();
    }

    public void removeArea(EditableArea area) throws UnsupportedAudioFileException, IOException {
        if (areas.entrySet().removeIf(e -> e.getValue() == area)) {
            areasChanged();
        }
    }

    public EditableArea newArea(int from, int to) {
        return new EditableArea(from, to, params);
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
            to = Math.min(range.to() + delta, source.frames);
        }
        return new EditableArea(from, to, params);
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
}
