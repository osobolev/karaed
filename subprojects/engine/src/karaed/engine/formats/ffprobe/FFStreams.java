package karaed.engine.formats.ffprobe;

import java.util.List;

public record FFStreams(
    List<FFStream> streams
) {}
