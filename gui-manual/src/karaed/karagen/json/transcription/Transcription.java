package karaed.karagen.json.transcription;

import java.util.List;

public record Transcription(
    String language,
    List<TransSegment> segments
) {}
