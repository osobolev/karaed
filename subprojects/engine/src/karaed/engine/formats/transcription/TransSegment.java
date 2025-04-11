package karaed.engine.formats.transcription;

public record TransSegment(
    double start,
    double end,
    String text
) {}
