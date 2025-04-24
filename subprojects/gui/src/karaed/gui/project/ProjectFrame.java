package karaed.gui.project;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.engine.formats.ranges.Range;
import karaed.gui.ErrorLogger;
import karaed.gui.align.ManualAlign;
import karaed.gui.options.OptionsDialog;
import karaed.gui.start.RecentItems;
import karaed.gui.start.StartFrame;
import karaed.gui.util.BaseFrame;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;
import karaed.gui.util.TitleUtil;
import karaed.project.*;
import karaed.tools.ProcRunner;
import karaed.tools.Tools;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ProjectFrame extends BaseFrame {

    private final Workdir workDir;
    private final ProcRunner runner;
    private final Runnable afterClose;

    private final JTextField tfTitle = new JTextField(40);
    private final Map<PipeStep, StepLabel> labels = new EnumMap<>(PipeStep.class);
    private final LogArea taLog = new LogArea();

    private volatile Thread runThread = null;

    private final Action runAction = new AbstractAction("Run") { // todo: add icon
        @Override
        public void actionPerformed(ActionEvent e) {
            runPipeline();
        }
    };
    private final Action stopAction = new AbstractAction("Stop") { // todo: add icon
        @Override
        public void actionPerformed(ActionEvent e) {
            if (runThread != null) {
                runThread.interrupt();
            }
        }
    };

    public static ProjectFrame create(ErrorLogger logger, boolean reopenStart, Tools tools, Path rootDir, Workdir workDir,
                                      Consumer<String> onError) {
        String error = RecentItems.isProjectDir(workDir);
        if (error != null) {
            onError.accept(error);
            return null;
        }
        RecentItems.addRecentItem(logger, workDir.dir());
        return new ProjectFrame(logger, tools, rootDir, workDir, reopenStart);
    }

    private ProjectFrame(ErrorLogger logger, Tools tools, Path rootDir, Workdir workDir, boolean reopenStart) {
        super(logger, "KaraEd");
        this.workDir = workDir;
        this.runner = new ProcRunner(tools, rootDir, taLog::append);
        this.afterClose = () -> {
            if (reopenStart) {
                new StartFrame(logger, tools, rootDir);
            }
        };

        JToolBar toolBar = new JToolBar();
        toolBar.add(new AbstractAction("Options") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OptionsDialog dlg = new OptionsDialog(logger, "Options", ProjectFrame.this, workDir);
                    if (dlg.isSaved()) {
                        refreshStepStates();
                    }
                } catch (Exception ex) {
                    error(ex);
                }
            }
        });
        toolBar.addSeparator();
        toolBar.add(runAction);
        toolBar.add(stopAction);
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
            StepLabel label = new StepLabel(step, this::fileClicked);
            label.setState(new RunStepState.NotRan());
            labels.put(step, label);
            steps.add(label.getVisual());
        }
        steps.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(steps, BorderLayout.CENTER);

        main.add(new JScrollPane(taLog.getVisual()), BorderLayout.SOUTH);

        enableDisable(false);
        refreshStepStates();

        pack();
        setLocationRelativeTo(null);
    }

    private void fileClicked(LinkType link) {
        try {
            if (link == LinkType.RANGES) {
                editRanges(false);
                return;
            }
            Path file = switch (link) {
                case AUDIO -> workDir.audio();
                case VIDEO -> workDir.video().getVideo("", false);
                case SUBS -> workDir.file("subs.ass");
                case KARAOKE -> workDir.file("karaoke.mp4");
                default -> null;
            };
            if (file != null && Files.exists(file)) {
                Desktop.getDesktop().open(file.toFile());
            } else {
                ShowMessage.error(this, "File not found");
            }
        } catch (Exception ex) {
            error(ex);
        }
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

    private void refreshStepStates() {
        try {
            PipeInfo pipe = PipeBuilder.create(workDir).buildPipe();
            showStepStates(pipe);
        } catch (Exception ex) {
            getLogger().error(ex);
        }
    }

    private void showStepStates(PipeInfo pipe) {
        for (Map.Entry<PipeStep, StepLabel> entry : labels.entrySet()) {
            PipeStep step = entry.getKey();
            StepLabel label = entry.getValue();
            RunStepState state = RunStepState.initState(pipe.stepStates().get(step));
            label.setHasVideo(pipe.hasVideo());
            label.setState(state);
        }
    }

    private void runPipeline() {
        PipeInfo pipe;
        try {
            pipe = PipeBuilder.create(workDir).buildPipe();
        } catch (Exception ex) {
            error(ex);
            return;
        }

        enableDisable(true);
        taLog.clear();
        showStepStates(pipe);

        Thread thread = new Thread(() -> {
            try {
                StepRunner stepRunner = new StepRunner(workDir, runner, this::showTitle, this::editRanges);
                long t0 = System.currentTimeMillis();
                boolean ok = true;
                for (PipeStep step : PipeStep.values()) {
                    StepState state = pipe.stepStates().get(step);
                    if (state instanceof StepState.Done)
                        continue;
                    setState(step, new RunStepState.Running());
                    try {
                        stepRunner.runStep(step);
                    } catch (Throwable ex) {
                        ok = false;
                        if (!(ex instanceof CancelledException || ex instanceof InterruptedException)) {
                            String message;
                            if (ex instanceof KaraException) {
                                message = ex.getMessage();
                                SwingUtilities.invokeLater(() -> ShowMessage.error(this, ex.getMessage()));
                            } else {
                                message = ex.toString();
                                SwingUtilities.invokeLater(() -> error(ex));
                            }
                            setState(step, new RunStepState.Error(message));
                            runner.println("ERROR: " + message);
                        } else {
                            setState(step, new RunStepState.NotRan());
                            runner.println("CANCELLED");
                        }
                        break;
                    }
                    setState(step, new RunStepState.Done());
                }
                long t1 = System.currentTimeMillis();
                if (ok) {
                    runner.println(String.format("DONE in %s", Range.formatTime((t1 - t0) / 1000.0f)));
                }
            } finally {
                runThread = null;
                SwingUtilities.invokeLater(() -> enableDisable(false));
            }
        }, "KaraEd pipe");
        runThread = thread;
        thread.start();
    }

    private void enableDisable(boolean running) {
        runAction.setEnabled(!running);
        stopAction.setEnabled(running);
    }

    private void editRanges() throws UnsupportedAudioFileException, IOException {
        if (!editRanges(true))
            throw new CancelledException();
    }

    private boolean editRanges(boolean canContinue) throws UnsupportedAudioFileException, IOException {
        Path ranges = workDir.file("ranges.json");
        Path vocals = workDir.vocals();
        Path text = workDir.file("text.txt");
        ManualAlign ma = ManualAlign.create(this, getLogger(), canContinue, vocals, text, ranges);
        ma.setVisible(true);
        return ma.isContinue();
    }

    @Override
    public boolean onClosing() {
        if (!runAction.isEnabled())
            return false;
        afterClose.run();
        return true;
    }
}
