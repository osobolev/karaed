package karaed.engine.formats.ranges;

import java.util.List;

public record Ranges(
    AreaParams params,
    List<Range> ranges,
    List<Area> areas,
    List<String> lines
) {

    public List<String> rangeLines() {
        return lines().stream().filter(line -> !line.trim().isEmpty()).toList();
    }
}
