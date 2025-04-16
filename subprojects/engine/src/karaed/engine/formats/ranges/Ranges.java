package karaed.engine.formats.ranges;

import java.util.List;

public record Ranges(
    float silenceThreshold,
    List<Range> ranges,
    List<String> lines
) {

    public List<String> rangeLines() {
        return lines().stream().filter(line -> !line.trim().isEmpty()).toList();
    }
}
