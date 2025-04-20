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
import karaed.tools.ProcRunner;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StepRunner {

    public interface EditRanges {

        void editRanges() throws Exception;
    }

    private final Workdir workDir;
    private final ProcRunner runner;
    private final Runnable showTitle;
    private final EditRanges editRanges;

    public StepRunner(Workdir workDir, ProcRunner runner, Runnable showTitle, EditRanges editRanges) {
        this.workDir = workDir;
        this.runner = runner;
        this.showTitle = showTitle;
        this.editRanges = editRanges;
    }

    public void runStep(PipeStep step) throws Throwable {
        switch (step) {
        case DOWNLOAD -> downloadAudio();
        case DEMUCS -> demucs();
        case RANGES -> ranges();
        case ALIGN -> align();
        case SUBS -> subs();
        case KARAOKE -> karaokeSubs();
        case PREPARE_VIDEO -> prepareVideo();
        case VIDEO -> karaokeVideo();
        }
    }

    private void downloadAudio() throws IOException, InterruptedException {
        Path audio = workDir.audio();
        Path info = workDir.info();
        OInput input = JsonUtil.readFile(workDir.file("input.json"), OInput.class);
        OCut cut = JsonUtil.readFile(workDir.option("cut.json"), OCut.class, OCut::new);
        try {
            Youtube.download(runner, input, cut, audio, info, workDir.video());
        } catch (InterruptedException ex) {
            Files.deleteIfExists(audio);
            Files.deleteIfExists(info);
            throw ex;
        }
        SwingUtilities.invokeLater(showTitle);
    }

    private void demucs() throws IOException, InterruptedException {
        Path configFile = workDir.option("demucs.json");
        ODemucs options = JsonUtil.readFile(configFile, ODemucs.class, ODemucs::new);
        try {
            Demucs.demucs(runner, workDir.audio(), options, workDir.dir());
        } catch (InterruptedException ex) {
            Files.deleteIfExists(workDir.vocals());
            Files.deleteIfExists(workDir.noVocals());
            throw ex;
        }
    }

    private void ranges() throws Throwable {
        if (SwingUtilities.isEventDispatchThread()) {
            editRanges.editRanges();
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        editRanges.editRanges();
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

    private void align() throws UnsupportedAudioFileException, IOException, InterruptedException {
        Path aligned = workDir.file("aligned.json");
        Path vocals = workDir.vocals();
        Path ranges = workDir.file("ranges.json");
        try {
            Align.align(runner, vocals, ranges, workDir.file("tmp"), aligned);
        } catch (InterruptedException ex) {
            Files.deleteIfExists(aligned);
            throw ex;
        }
    }

    private void subs() throws IOException {
        Path subs = workDir.file("subs.ass");
        Path text = workDir.file("text.txt");
        Path aligned = workDir.file("aligned.json");
        OAlign options = JsonUtil.readFile(workDir.option("align.json"), OAlign.class, OAlign::new);
        MakeSubs.makeSubs(text, aligned, options, subs);
    }

    private void karaokeSubs() throws IOException {
        Path karaoke = workDir.file("karaoke.ass");
        Path subs = workDir.file("subs.ass");
        Path info = workDir.info();
        OKaraoke options = JsonUtil.readFile(workDir.option("karaoke.json"), OKaraoke.class, OKaraoke::new);
        AssJoiner.join(subs, info, options, karaoke);
    }

    private void prepareVideo() throws IOException, InterruptedException {
        OVideo options = JsonUtil.readFile(workDir.option("video.json"), OVideo.class, OVideo::new);
        if (options.useOriginalVideo()) {
            VideoFinder finder = workDir.video();
            try {
                MakeVideo.prepareVideo(runner, finder);
            } catch (InterruptedException ex) {
                Path preparedVideo = finder.getVideo(MakeVideo.PREPARED, false);
                if (preparedVideo != null) {
                    Files.deleteIfExists(preparedVideo);
                }
                throw ex;
            }
        }
    }

    private void karaokeVideo() throws IOException, InterruptedException {
        Path karaokeVideo = workDir.file("karaoke.mp4");
        Path noVocals = workDir.noVocals();
        Path karaoke = workDir.file("karaoke.ass");
        OVideo options = JsonUtil.readFile(workDir.option("video.json"), OVideo.class, OVideo::new);
        Path video = options.useOriginalVideo() ? MakeVideo.getVideo(workDir.video()) : null;
        try {
            MakeVideo.karaokeVideo(runner, video, noVocals, karaoke, karaokeVideo);
        } catch (InterruptedException ex) {
            Files.deleteIfExists(karaokeVideo);
            throw ex;
        }
    }
}
