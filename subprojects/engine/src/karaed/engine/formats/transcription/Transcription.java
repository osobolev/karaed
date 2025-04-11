package karaed.engine.formats.transcription;

import java.util.List;

public record Transcription(
    String language,
    List<TransSegment> segments
) {}
