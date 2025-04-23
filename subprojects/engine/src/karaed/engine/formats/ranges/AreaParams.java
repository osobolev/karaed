package karaed.engine.formats.ranges;

public record AreaParams(
    int silenceThreshold,
    float maxSilenceGap,
    float minRangeDuration
) {}
