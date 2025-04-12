package karaed.engine.formats.ranges;

import java.util.List;

public record Ranges(
    List<Range> ranges,
    List<String> lines
) {}
