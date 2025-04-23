package karaed.engine.audio;

public interface RangeParams {

    float silenceThreshold(int frame);

    int maxSilenceGap(int frame);

    int minRangeDuration(int frame);
}
