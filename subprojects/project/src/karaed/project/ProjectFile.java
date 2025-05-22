package karaed.project;

import java.util.List;

enum ProjectFile {
    INPUT,
    TEXT,
    ORIGINAL_VIDEO("cut.json"),
    AUDIO("cut.json"),
    INFO,
    PREPARED_VIDEO,
    VOCALS("demucs.json"),
    NO_VOCALS("demucs.json"),
    RANGES,
    LANGUAGE,
    ALIGNED,
    SUBS("align.json"),
    KARAOKE_SUBS("karaoke.json"),
    KARAOKE_VIDEO("video.json");

    final List<String> options;

    ProjectFile(String... options) {
        this.options = List.of(options);
    }
}
