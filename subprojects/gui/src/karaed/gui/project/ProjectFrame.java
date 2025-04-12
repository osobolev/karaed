package karaed.gui.project;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.engine.opts.ODemucs;
import karaed.engine.opts.OInput;
import karaed.engine.steps.align.Align;
import karaed.engine.steps.demucs.Demucs;
import karaed.engine.steps.youtube.Youtube;
import karaed.gui.ErrorLogger;
import karaed.gui.align.ManualAlign;
import karaed.gui.options.OptionsDialog;
import karaed.gui.start.RecentItems;
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

// todo: options/cut.json - what piece of original file to use (from-to)
// todo: options/demucs.json - number of shifts
// todo: options/ranges.json - auto/manual
// todo: options/align.json - by words/chars
// todo: options/karaoke.json - karaoke generation properties
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
        return new ProjectFrame(logger, tools, rootDir, workDir);
    }

    private ProjectFrame(ErrorLogger logger, Tools tools, Path rootDir, Workdir workDir) {
        super("KaraEd");
        this.logger = logger;
        this.workDir = workDir;
        this.runner = new ProcRunner(tools, rootDir, taLog::append);

        JToolBar toolBar = new JToolBar();
        toolBar.add(new AbstractAction("Options") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new OptionsDialog(logger, ProjectFrame.this, workDir);
                } catch (Exception ex) {
                    ShowMessage.error(ProjectFrame.this, logger, ex);
                }
            }
        });
        toolBar.addSeparator();
        toolBar.add(runAction);
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

        // todo: reopen start frame if was opened from start
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
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
        Youtube.download(runner, input, audio);
        SwingUtilities.invokeLater(this::showTitle);
    }

    private void demucs() throws IOException, InterruptedException {
        Path vocals = workDir.demuxed("vocals.wav");
        Path noVocals = workDir.demuxed("no_vocals.wav");
        if (Files.exists(vocals) && Files.exists(noVocals)) // todo: check audio.mp3 + options/demucs.json
            return;
        Path configFile = workDir.option("demucs.json");
        ODemucs options = JsonUtil.readFile(configFile, ODemucs.class, ODemucs::new);
        Demucs.demucs(runner, options, workDir.audio(), workDir.dir());
    }

    private void ranges() throws Throwable {
        Path ranges = workDir.file("ranges.json");
        if (Files.exists(ranges)) // todo: check text.txt + vocals.wav + options/ranges.json
            return;
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

    private void subs() {
        // todo
    }

    private void karaokeSubs() {
        // todo
    }

    private void karaokeVideo() {
        // todo
    }
}
