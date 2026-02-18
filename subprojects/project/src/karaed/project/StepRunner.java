package karaed.project;

import karaed.engine.opts.*;
import karaed.engine.steps.align.Align;
import karaed.engine.steps.demucs.Demucs;
import karaed.engine.steps.karaoke.AssJoiner;
import karaed.engine.steps.subs.MakeSubs;
import karaed.engine.steps.video.MakeVideo;
import karaed.engine.steps.youtube.Youtube;
import karaed.engine.video.VideoFinder;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StepRunner {

    public interface Editor {

        void openEditor() throws Exception;
    }

    private final Workdir workDir;
    private final ToolRunner runner;
    private final Runnable showTitle;
    private final Editor editRanges;
    private final Editor editBackvocals;

    public StepRunner(Workdir workDir, ToolRunner runner, Runnable showTitle,
                      Editor editRanges, Editor editBackvocals) {
        this.workDir = workDir;
        this.runner = runner;
        this.showTitle = showTitle;
        this.editRanges = editRanges;
        this.editBackvocals = editBackvocals;
    }

    private static void runEditor(Editor editor) throws Throwable {
        if (SwingUtilities.isEventDispatchThread()) {
            editor.openEditor();
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        editor.openEditor();
                    } catch (Exception ex) {
                        throw new WrapException(ex);
                    }
                });
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof WrapException we) {
                    throw we.getCause();
                } else {
                    throw cause;
                }
            }
        }
    }

    public void runStep(PipeStep step) throws Throwable {
        switch (step) {
        case DOWNLOAD -> downloadAudio();
        case DEMUCS -> demucs();
        case RANGES -> runEditor(editRanges);
        case ALIGN -> align();
        case SUBS -> subs();
        case BACKVOCALS -> runEditor(editBackvocals);
        case KARAOKE -> karaokeSubs();
        case PREPARE_VIDEO -> prepareVideo();
        case VIDEO -> karaokeVideo();
        }
    }

    private void downloadAudio() throws IOException, InterruptedException {
        Path audio = workDir.audio();
        Path info = workDir.info();
        OInput input = JsonUtil.readFile(workDir.file("input.json"), OInput.class);
        OCut cut = workDir.option("cut.json", OCut.class, OCut::new);
        try {
            Youtube.download(runner, input, cut, audio, info, workDir.video());
        } catch (IOException | InterruptedException ex) {
            deleteIfExists(audio);
            deleteIfExists(info);
            throw ex;
        }
        SwingUtilities.invokeLater(showTitle);
    }

    private void demucs() throws IOException, InterruptedException {
        ODemucs options = workDir.option("demucs.json", ODemucs.class, ODemucs::new);
        try {
            Demucs.demucs(runner, workDir.audio(), options, workDir.dir());
        } catch (IOException | InterruptedException ex) {
            deleteIfExists(workDir.vocals());
            deleteIfExists(workDir.noVocals());
            throw ex;
        }
    }

    private void align() throws UnsupportedAudioFileException, IOException, InterruptedException {
        Path aligned = workDir.file("aligned.json");
        Path vocals = workDir.vocals();
        Path ranges = workDir.file("ranges.json");
        Path lang = workDir.file("lang.json");
        try {
            Align.align(runner, vocals, ranges, lang, workDir.file("tmp"), aligned);
        } catch (IOException | InterruptedException ex) {
            deleteIfExists(aligned);
            throw ex;
        }
    }

    private void subs() throws IOException {
        Path subs = workDir.file("subs.ass");
        Path backvocals = workDir.file("backvocals.json");
        Path text = workDir.file("text.txt");
        Path aligned = workDir.file("aligned.json");
        OAlign options = workDir.option("align.json", OAlign.class, OAlign::new);
        runner.println("Making editable subtitles");
        MakeSubs.makeSubs(text, aligned, options, subs, backvocals);
    }

    private void karaokeSubs() throws IOException {
        Path karaoke = workDir.file("karaoke.ass");
        Path subs = workDir.file("subs.ass");
        Path info = workDir.info();
        OKaraoke options = workDir.option("karaoke.json", OKaraoke.class, OKaraoke::new);
        runner.println("Making karaoke subtitles");
        AssJoiner.join(subs, info, options, karaoke);
    }

    private void prepareVideo() throws IOException, InterruptedException {
        OVideo options = workDir.option("video.json", OVideo.class, OVideo::new);
        if (options.useOriginalVideo()) {
            VideoFinder finder = workDir.video();
            try {
                MakeVideo.prepareVideo(runner, finder);
            } catch (IOException | InterruptedException ex) {
                Path preparedVideo = finder.getVideo(MakeVideo.PREPARED, false);
                if (preparedVideo != null) {
                    deleteIfExists(preparedVideo);
                }
                throw ex;
            }
        }
    }

    private void karaokeVideo() throws IOException, InterruptedException {
        Path karaokeVideo = workDir.file("karaoke.mp4");

        Path noVocals = workDir.noVocals();
        Path karaoke = workDir.file("karaoke.ass");
        OVideo options = workDir.option("video.json", OVideo.class, OVideo::new);
        Path video = options.useOriginalVideo() ? MakeVideo.getVideo(workDir.video()) : null;

        Path vocals = workDir.vocals();
        Path backvocals = workDir.file("backvocals.json");

        try {
            MakeVideo.karaokeVideo(
                runner, video, noVocals, karaoke,
                vocals, backvocals,
                karaokeVideo
            );
        } catch (IOException | InterruptedException ex) {
            deleteIfExists(karaokeVideo);
            throw ex;
        }
    }

    private static void deleteIfExists(Path path) {
        for (int i = 0; i < 2; i++) {
            try {
                Files.deleteIfExists(path);
                break;
            } catch (IOException ex2) {
                // ignore
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
