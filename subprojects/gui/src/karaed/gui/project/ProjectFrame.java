package karaed.gui.project;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.gui.ErrorLogger;
import karaed.gui.align.ManualAlign;
import karaed.gui.options.OptionsDialog;
import karaed.gui.start.RecentItems;
import karaed.gui.start.StartFrame;
import karaed.gui.util.CloseUtil;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;
import karaed.gui.util.TitleUtil;
import karaed.project.PipeBuilder;
import karaed.project.PipeStep;
import karaed.project.StepRunner;
import karaed.project.StepState;
import karaed.tools.ProcRunner;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
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
            label.setState(new RunStepState.NotRan());
            labels.put(step, label);
            steps.add(label.getVisual());
        }
        steps.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(steps, BorderLayout.CENTER);

        main.add(new JScrollPane(taLog.getVisual()), BorderLayout.SOUTH);

        try {
            Map<PipeStep, StepState> pipe = PipeBuilder.create(workDir).buildPipe();
            showStepStates(pipe);
        } catch (Exception ex) {
            logger.error(ex);
        }

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

    private void setState(PipeStep step, RunStepState state) {
        SwingUtilities.invokeLater(() -> {
            StepLabel label = labels.get(step);
            label.setState(state);
        });
    }

    private void showStepStates(Map<PipeStep, StepState> pipe) {
        for (Map.Entry<PipeStep, StepLabel> entry : labels.entrySet()) {
            PipeStep step = entry.getKey();
            StepLabel label = entry.getValue();
            label.setState(RunStepState.initState(pipe.get(step)));
        }
    }

    private void runPipeline() {
        Map<PipeStep, StepState> pipe;
        try {
            pipe = PipeBuilder.create(workDir).buildPipe();
        } catch (Exception ex) {
            ShowMessage.error(this, logger, ex);
            return;
        }

        runAction.setEnabled(false);
        taLog.clear();
        showStepStates(pipe);

        Thread thread = new Thread(() -> {
            try {
                StepRunner stepRunner = new StepRunner(workDir, runner, this::showTitle, this::editRanges);
                for (PipeStep step : PipeStep.values()) {
                    StepState state = pipe.get(step);
                    if (state instanceof StepState.Done)
                        continue;
                    setState(step, new RunStepState.Running());
                    try {
                        stepRunner.runStep(step);
                    } catch (Throwable ex) {
                        if (!(ex instanceof CancelledException)) {
                            String message;
                            if (ex instanceof KaraException) {
                                message = ex.getMessage();
                                SwingUtilities.invokeLater(() -> ShowMessage.error(this, ex.getMessage()));
                            } else {
                                message = ex.toString();
                                SwingUtilities.invokeLater(() -> ShowMessage.error(this, logger, ex));
                            }
                            setState(step, new RunStepState.Error(message));
                        }
                        break;
                    }
                    setState(step, new RunStepState.Done());
                }
            } finally {
                SwingUtilities.invokeLater(() -> runAction.setEnabled(true));
            }
        }, "KaraEd pipe");
        thread.start();
    }

    private void editRanges() throws UnsupportedAudioFileException, IOException {
        Path ranges = workDir.file("ranges.json");
        Path vocals = workDir.vocals();
        Path text = workDir.file("text.txt");
        ManualAlign ma = ManualAlign.create(this, logger, vocals, text, ranges);
        ma.setVisible(true);
        if (!ma.isOK())
            throw new CancelledException();
    }
}
