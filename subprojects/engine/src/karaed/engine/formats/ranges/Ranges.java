package karaed.engine.formats.ranges;

import java.util.List;

public record Ranges(
    float silenceThreshold,
    List<Range> ranges,
    List<String> lines
) {}
