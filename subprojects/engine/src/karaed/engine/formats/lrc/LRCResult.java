package karaed.engine.formats.lrc;

public record LRCResult(
    String artistName,
    String albumName,
    String trackName,
    Double duration,
    String plainLyrics
) {}
