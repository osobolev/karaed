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
import karaed.workdir.Workdir;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
        OInput input = JsonUtil.readFile(workDir.file("input.json"), OInput.class);
        OCut cut = JsonUtil.readFile(workDir.option("cut.json"), OCut.class, OCut::new);
        Youtube.download(runner, input, cut, audio);
        SwingUtilities.invokeLater(showTitle);
    }

    private void demucs() throws IOException, InterruptedException {
        Path configFile = workDir.option("demucs.json");
        ODemucs options = JsonUtil.readFile(configFile, ODemucs.class, ODemucs::new);
        Demucs.demucs(runner, workDir.audio(), options, workDir.dir());
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
        Align.align(runner, vocals, ranges, workDir.file("tmp"), aligned);
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
        VideoFinder finder = MakeVideo.prepareVideo(workDir.audio(), options);
        if (finder == null)
            return;
        MakeVideo.prepareVideo(runner, finder);
    }

    private void karaokeVideo() throws IOException, InterruptedException {
        Path karaokeVideo = workDir.file("karaoke.mp4");
        Path noVocals = workDir.demuxed("no_vocals.wav");
        Path karaoke = workDir.file("karaoke.ass");
        OVideo options = JsonUtil.readFile(workDir.option("video.json"), OVideo.class, OVideo::new);
        MakeVideo.karaokeVideo(runner, workDir.audio(), noVocals, karaoke, options, karaokeVideo);
    }
}
