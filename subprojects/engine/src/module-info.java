module karaed.engine {
    requires transitive java.desktop;
    requires transitive karaed.tools;

    requires java.net.http;
    requires io.github.osobolev.ass;
    requires karaed.json;

    exports karaed.engine;
    exports karaed.engine.ass;
    exports karaed.engine.audio;
    exports karaed.engine.formats.backvocals;
    exports karaed.engine.formats.info;
    exports karaed.engine.formats.ranges;
    exports karaed.engine.lyrics;
    exports karaed.engine.opts;
    exports karaed.engine.steps.align;
    exports karaed.engine.steps.demucs;
    exports karaed.engine.steps.karaoke;
    exports karaed.engine.steps.subs;
    exports karaed.engine.steps.video;
    exports karaed.engine.steps.youtube;
    exports karaed.engine.video;
}
