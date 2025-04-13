package karaed.engine.formats.ffprobe;

public record FFFrame(
    String best_effort_timestamp_time,
    String pict_type
) {}
