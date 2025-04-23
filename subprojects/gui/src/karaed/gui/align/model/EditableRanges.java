package karaed.gui.align.model;

import karaed.engine.audio.MaxAudioSource;
import karaed.engine.audio.RangeParams;
import karaed.engine.audio.VoiceRanges;
import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class EditableRanges {

    public final MaxAudioSource source;

    private AreaParams params;
    private final List<Range> ranges;
    private final List<Area> areas;

    private final List<RangeEditListener> listeners = new ArrayList<>();

    public EditableRanges(MaxAudioSource source,
                          AreaParams params, List<Range> ranges, List<Area> areas) {
        this.source = source;
        this.params = params;
        this.ranges = new ArrayList<>(ranges);
        this.areas = new ArrayList<>(areas);
    }

    public void splitByParams(AreaParams params) throws UnsupportedAudioFileException, IOException {
        List<Range> ranges = VoiceRanges.detectVoice(source, getRangeParams(params));
        this.params = params;
        if (!this.ranges.equals(ranges)) {
            this.ranges.clear();
            this.ranges.addAll(ranges);
            fireChanged(true);
        }
    }

    private RangeParams getRangeParams(AreaParams params) {
        float frameRate = source.format.getFrameRate();
        int maxSilenceGap = (int) (params.maxSilenceGap() * frameRate);
        int minRangeDuration = (int) (params.minRangeDuration() * frameRate);
        // todo: do not apply params to areas???
        return new RangeParams() {

            @Override
            public float silenceThreshold(int frame) {
                return params.silenceThreshold();
            }

            @Override
            public int maxSilenceGap(int frame) {
                return maxSilenceGap; // todo
            }

            @Override
            public int minRangeDuration(int frame) {
                return minRangeDuration; // todo
            }
        };
    }

    public void addArea(Area area) {
        // todo: make sure areas do not intersect
        // todo: sort areas!!!
        areas.add(area);
        // todo: re-split according to area params???
        fireChanged(false); // todo: can be true if ranges changes after resplit!!!
    }

    public void removeArea(Area area) {
        if (areas.remove(area)) {
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

    public List<Area> getAreas() {
        return areas;
    }
}
