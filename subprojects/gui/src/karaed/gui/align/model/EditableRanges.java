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

    public void splitByParams(EditableArea area, AreaParams params) throws UnsupportedAudioFileException, IOException {
        if (area == null) {
            this.params = params;
        } else {
            area.params = params;
        }
        // todo: rollback params assignment on error???
        List<Range> ranges = VoiceRanges.detectVoice(source, getRangeParams());
        if (!this.ranges.equals(ranges)) {
            this.ranges.clear();
            this.ranges.addAll(ranges);
            fireChanged(true);
        }
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

    public void addArea(int from, int to, AreaParams params) {
        // todo: make sure areas do not intersect
        // todo: sort areas!!!
        areas.put(from, new EditableArea(from, to, params));
        // todo: re-split according to area params???
        fireChanged(false); // todo: can be true if ranges changes after resplit!!!
    }

    public void removeArea(EditableArea area) {
        if (areas.entrySet().removeIf(e -> e.getValue() == area)) {
            // todo: re-split according to global params???
            fireChanged(false); // todo: can be true if ranges changes after resplit!!!
        }
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

    public List<Range> getRanges() {
        return ranges;
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
