package karaed.engine.audio;

public interface RangeParams {

    int silenceThreshold(int frame);

    int maxSilenceGap(int frame);

    int minRangeDuration(int frame);
}
