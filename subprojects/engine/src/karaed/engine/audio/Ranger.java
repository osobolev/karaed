package karaed.engine.audio;

import karaed.engine.formats.ranges.Range;

import java.util.ArrayList;
import java.util.List;

final class Ranger {

    private final RangeParams params;

    private int voiceStarted = -1;
    private final List<Range> ranges = new ArrayList<>();

    Ranger(RangeParams params) {
        this.params = params;
    }

    private void finishRange(int frame) {
        ranges.add(new Range(voiceStarted, frame));
        voiceStarted = -1;
    }

    void add(int frame, boolean silence) {
        if (voiceStarted >= 0) {
            if (silence) {
                finishRange(frame);
            }
        } else {
            if (!silence) {
                if (!ranges.isEmpty()) {
                    int lastIndex = ranges.size() - 1;
                    Range prev = ranges.get(lastIndex);
                    int silenceSize = frame - prev.to();
                    if (silenceSize < params.maxSilenceGap(frame)) {
                        ranges.remove(lastIndex);
                        voiceStarted = prev.from();
                        return;
                    }
                }
                voiceStarted = frame;
            }
        }
    }

    List<Range> finish(int frame) {
        if (voiceStarted >= 0) {
            finishRange(frame);
        }
        ranges.removeIf(
            range -> range.to() - range.from() < params.minRangeDuration(Range.mid(range.from(), range.to()))
        );
        return ranges;
    }
}
