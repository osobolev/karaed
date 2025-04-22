package karaed.engine.formats.ranges;

import java.util.List;

public record Ranges(
    AreaParams params,
    List<Range> ranges, // todo: no need to store ranges, auto-detect them from areas???
    List<Area> areas,
    List<String> lines
) {

    public List<String> rangeLines() {
        return lines().stream().filter(line -> !line.trim().isEmpty()).toList();
    }
}
