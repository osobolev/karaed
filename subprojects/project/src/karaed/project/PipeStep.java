package karaed.project;

public enum PipeStep {
    DOWNLOAD("Downloading audio/video"),
    DEMUCS("Separating vocals"),
    RANGES("Detecting ranges"),
    ALIGN("Aligning vocals with lyrics"),
    SUBS("Making editable subtitles"),
    KARAOKE("Making karaoke subtitles"),
    PREPARE_VIDEO("Preparing video"),
    VIDEO("Making karaoke video");

    public final String text;

    PipeStep(String text) {
        this.text = text;
    }
}
