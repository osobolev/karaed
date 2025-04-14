package karaed.gui.project;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.engine.opts.*;
import karaed.engine.steps.align.Align;
import karaed.engine.steps.demucs.Demucs;
import karaed.engine.steps.karaoke.AssJoiner;
import karaed.engine.steps.subs.MakeSubs;
import karaed.engine.steps.video.MakeVideo;
import karaed.engine.steps.youtube.Youtube;
import karaed.gui.ErrorLogger;
import karaed.gui.align.ManualAlign;
import karaed.gui.options.OptionsDialog;
import karaed.gui.start.RecentItems;
import karaed.gui.start.StartFrame;
import karaed.gui.util.CloseUtil;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;
import karaed.gui.util.TitleUtil;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

// todo: options/ranges.json - auto/manual
// todo: options/align.json - by words/chars
public final class ProjectFrame extends JFrame {

    private final ErrorLogger logger;
    private final Workdir workDir;
    private final ProcRunner runner;

    private final JTextField tfTitle = new JTextField(40);
    private final Map<PipeStep, StepLabel> labels = new EnumMap<>(PipeStep.class);
    private final LogArea taLog = new LogArea();

    private final Action runAction = new AbstractAction("Run") {
        @Override
        public void actionPerformed(ActionEvent e) {
            runPipeline();
        }
    };

    public static ProjectFrame create(ErrorLogger logger, Window current, Tools tools, Path rootDir, Workdir workDir) {
        String error = RecentItems.isProjectDir(workDir);
        if (error != null) {
            ShowMessage.error(current, error);
            return null;
        }
        RecentItems.addRecentItem(logger, workDir.dir());
        return new ProjectFrame(logger, tools, rootDir, workDir, current != null);
    }

    private ProjectFrame(ErrorLogger logger, Tools tools, Path rootDir, Workdir workDir, boolean reopenStart) {
        super("KaraEd");
        this.logger = logger;
        this.workDir = workDir;
        this.runner = new ProcRunner(tools, rootDir, taLog::append);

        JToolBar toolBar = new JToolBar();
        toolBar.add(new AbstractAction("Options") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new OptionsDialog(logger, "Options", ProjectFrame.this, workDir);
                } catch (Exception ex) {
                    ShowMessage.error(ProjectFrame.this, logger, ex);
                }
            }
        });
        toolBar.addSeparator();
        toolBar.add(runAction);
        // todo: stop button
        add(toolBar, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        add(main, BorderLayout.CENTER);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JTextField tfPath = new JTextField(40);
        tfPath.setEditable(false);
        InputUtil.setText(tfPath, workDir.dir().toString());

        tfTitle.setEditable(false);
        showTitle();

        top.add(tfPath);
        top.add(tfTitle);
        main.add(top, BorderLayout.NORTH);

        JPanel steps = new JPanel();
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        for (PipeStep step : PipeStep.values()) {
            StepLabel label = new StepLabel(step);
            label.setState(StepState.INIT);
            labels.put(step, label);
            steps.add(label.getVisual());
        }
        steps.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(steps, BorderLayout.CENTER);

        main.add(new JScrollPane(taLog.getVisual()), BorderLayout.SOUTH);

        CloseUtil.listen(this, () -> {
            // todo: do not close if running
            if (reopenStart) {
                new StartFrame(logger, tools, rootDir);
            }
            return true;
        });
        pack();
        setLocationRelativeTo(null);
    }

    private void showTitle() {
        Info info = TitleUtil.getInfo(workDir);
        if (info != null) {
            InputUtil.setText(tfTitle, info.toString());
        }
    }

    private void setState(PipeStep step, StepState state) {
        SwingUtilities.invokeLater(() -> {
            StepLabel label = labels.get(step);
            label.setState(state);
        });
    }

    private void runPipeline() {
        runAction.setEnabled(false);
        taLog.clear();
        for (StepLabel label : labels.values()) {
            label.setState(StepState.INIT);
        }
        Thread thread = new Thread(() -> {
            try {
                setState(PipeStep.DOWNLOAD, StepState.RUNNING);
                downloadAudio();
                setState(PipeStep.DOWNLOAD, StepState.COMPLETE);

                setState(PipeStep.DEMUCS, StepState.RUNNING);
                demucs();
                setState(PipeStep.DEMUCS, StepState.COMPLETE);

                setState(PipeStep.RANGES, StepState.RUNNING);
                ranges();
                setState(PipeStep.RANGES, StepState.COMPLETE);

                setState(PipeStep.ALIGN, StepState.RUNNING);
                align();
                setState(PipeStep.ALIGN, StepState.COMPLETE);

                setState(PipeStep.SUBS, StepState.RUNNING);
                subs();
                setState(PipeStep.SUBS, StepState.COMPLETE);

                setState(PipeStep.KARAOKE, StepState.RUNNING);
                karaokeSubs();
                setState(PipeStep.KARAOKE, StepState.COMPLETE);

                setState(PipeStep.PREPARE_VIDEO, StepState.RUNNING);
                prepareVideo();
                setState(PipeStep.PREPARE_VIDEO, StepState.COMPLETE);

                setState(PipeStep.VIDEO, StepState.RUNNING);
                karaokeVideo();
                setState(PipeStep.VIDEO, StepState.COMPLETE);
            } catch (Throwable ex) {
                // todo: mark current pipe stage as bad!!!
                if (ex instanceof KaraException) {
                    SwingUtilities.invokeLater(() -> ShowMessage.error(this, ex.getMessage()));
                } else if (!(ex instanceof CancelledException)) {
                    SwingUtilities.invokeLater(() -> ShowMessage.error(this, logger, ex));
                }
            } finally {
                SwingUtilities.invokeLater(() -> runAction.setEnabled(true));
            }
        }, "KaraEd pipe");
        thread.start();
    }

    private void downloadAudio() throws IOException, InterruptedException {
        Path audio = workDir.audio();
        if (Files.exists(audio)) // todo: check input.json + options/cut.json
            return;
        OInput input = JsonUtil.readFile(workDir.file("input.json"), OInput.class);
        OCut cut = JsonUtil.readFile(workDir.option("cut.json"), OCut.class, OCut::new);
        Youtube.download(runner, input, cut, audio);
        SwingUtilities.invokeLater(this::showTitle);
    }

    private void demucs() throws IOException, InterruptedException {
        Path vocals = workDir.demuxed("vocals.wav");
        Path noVocals = workDir.demuxed("no_vocals.wav");
        if (Files.exists(vocals) && Files.exists(noVocals)) // todo: check audio.mp3 + options/demucs.json
            return;
        Path configFile = workDir.option("demucs.json");
        ODemucs options = JsonUtil.readFile(configFile, ODemucs.class, ODemucs::new);
        Demucs.demucs(runner, workDir.audio(), options, workDir.dir());
    }

    private void ranges() throws Throwable {
        Path ranges = workDir.file("ranges.json");
        Path vocals = workDir.vocals();
        Path text = workDir.file("text.txt");
        Runnable editRanges = () -> {
            ManualAlign ma;
            try {
                ma = ManualAlign.create(this, logger, vocals, text, ranges);
            } catch (Exception ex) {
                throw new WrapException(ex);
            }
            ma.setVisible(true);
            if (!ma.isOK())
                throw new CancelledException();
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                editRanges.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(editRanges);
                } catch (InvocationTargetException ex) {
                    throw ex.getCause();
                }
            }
        } catch (WrapException ex) {
            throw ex.getCause();
        }
    }

    private void align() throws UnsupportedAudioFileException, IOException, InterruptedException {
        Path aligned = workDir.file("aligned.json");
        if (Files.exists(aligned)) // todo: check audio.mp3 + ranges.json
            return;
        Path vocals = workDir.vocals();
        Path ranges = workDir.file("ranges.json");
        Align.align(runner, vocals, ranges, workDir.file("tmp"), aligned);
    }

    private void subs() throws IOException {
        Path subs = workDir.file("subs.ass");
        if (Files.exists(subs)) // todo: check text.txt + aligned.json + options/align.json
            return;
        Path text = workDir.file("text.txt");
        Path aligned = workDir.file("aligned.json");
        MakeSubs.makeSubs(text, aligned, subs);
    }

    private void karaokeSubs() throws IOException {
        Path karaoke = workDir.file("karaoke.ass");
        if (Files.exists(karaoke)) // todo: check subs.ass + info.json + options/karaoke.json
            return;
        Path subs = workDir.file("subs.ass");
        Path info = workDir.info();
        OKaraoke options = JsonUtil.readFile(workDir.option("karaoke.json"), OKaraoke.class, OKaraoke::new);
        AssJoiner.join(subs, info, options, karaoke);
    }

    private void prepareVideo() throws IOException, InterruptedException {
        OVideo options = JsonUtil.readFile(workDir.option("video.json"), OVideo.class, OVideo::new);
        MakeVideo.Preparer preparer = MakeVideo.prepareVideo(workDir.audio(), options);
        if (preparer == null)
            return;
        Path preparedVideo = preparer.getPreparedVideo();
        if (Files.exists(preparedVideo)) // todo: check original video
            return;
        preparer.prepare(runner);
    }

    private void karaokeVideo() throws IOException, InterruptedException {
        Path karaokeVideo = workDir.file("karaoke.mp4");
        if (Files.exists(karaokeVideo)) // todo: check no_vocals.wav + karaoke.ass + video (chosen if needed) + options/video.json
            return;
        Path noVocals = workDir.demuxed("no_vocals.wav");
        Path karaoke = workDir.file("karaoke.ass");
        OVideo options = JsonUtil.readFile(workDir.option("video.json"), OVideo.class, OVideo::new);
        MakeVideo.karaokeVideo(runner, workDir.audio(), noVocals, karaoke, options, karaokeVideo);
    }
}
