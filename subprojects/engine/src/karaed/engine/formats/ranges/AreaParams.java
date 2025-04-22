package karaed.engine.formats.ranges;

public record AreaParams(
    float silenceThreshold,
    float maxSilenceGap,
    float minRangeDuration
) {}
