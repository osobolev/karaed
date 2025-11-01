package karaed.gui.tools;

import java.util.Objects;

enum Tool {
    PYTHON("Python"),
    PIP("pip"),
    FFMPEG("ffmpeg"),
    YT_DLP("yt-dlp"),
    DEMUCS("demucs"),
    WHISPERX("whisperx");

    private final String text;

    Tool(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    String maybePackName() {
        return switch (this) {
            case PIP -> "pip";
            case YT_DLP -> "yt-dlp";
            case DEMUCS -> "demucs";
            case WHISPERX -> "whisperx";
            default -> null;
        };
    }

    String packName() {
        String packName = maybePackName();
        return Objects.requireNonNull(packName, () -> name() + " is not a Python package");
    }
}
