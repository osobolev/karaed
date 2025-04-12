package karaed.gui.project;

enum PipeStep {
    DOWNLOAD("Downloading audio/video"),
    DEMUCS("Separating vocals"),
    RANGES("Detecting ranges"),
    ALIGN("Aligning vocals with lyrics"),
    SUBS("Making editable subtitles"),
    KARAOKE("Making karaoke subtitles"),
    VIDEO("Making karaoke video");

    final String text;

    PipeStep(String text) {
        this.text = text;
    }
}
