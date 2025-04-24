package karaed.engine.formats.ranges;

public record Area(
    int from,
    int to,
    AreaParams params
) implements RangeLike {}
