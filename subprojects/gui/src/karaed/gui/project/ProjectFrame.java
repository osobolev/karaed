package karaed.gui.project;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.engine.formats.ranges.Range;
import karaed.gui.ErrorLogger;
import karaed.gui.align.ManualAlign;
import karaed.gui.backvocals.EditBackvocals;
import karaed.gui.options.OptionsDialog;
import karaed.gui.start.DirStatus;
import karaed.gui.start.RecentItems;
import karaed.gui.start.StartFrame;
import karaed.gui.tools.ToolsDialog;
import karaed.gui.util.*;
import karaed.project.*;
import karaed.tools.CommandException;
import karaed.tools.ToolRunner;
import karaed.tools.Tools;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ProjectFrame extends BaseFrame {

    private static final Icon ICON_PLAY = InputUtil.getIcon("/play.png");
    private static final Icon ICON_STOP = InputUtil.getIcon("/stop.png");

    private final Workdir workDir;
    private final ToolRunner runner;
    private final Runnable afterClose;

    private final JTextField tfTitle = new JTextField(40);
    private final Map<PipeStep, StepLabel> labels = new EnumMap<>(PipeStep.class);
    private final LogArea taLog = new LogArea();

    private volatile Thread runThread = null;

    private final Action runAction = new AbstractAction("Run", ICON_PLAY) {
        @Override
        public void actionPerformed(ActionEvent e) {
            runPipeline();
        }
    };
    private final Action stopAction = new AbstractAction("Stop", ICON_STOP) {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (runThread != null) {
                runThread.interrupt();
            }
        }
    };

    public static JButton createToolsButton(BaseWindow owner, Tools tools) {
        JButton btnTools = new JButton(InputUtil.getIcon("/tools.png"));
        btnTools.addActionListener(e -> new ToolsDialog(owner.getLogger(), owner.toWindow(), false, tools));
        btnTools.setToolTipText("Tools setup");
        btnTools.setMargin(new Insets(0, 3, 0, 3));
        return btnTools;
    }

    public static ProjectFrame create(ErrorLogger logger, boolean reopenStart, Tools tools, Path rootDir, Workdir workDir,
                                      Consumer<String> onError) {
        DirStatus status = DirStatus.test(workDir);
        if (status != DirStatus.OK) {
            onError.accept(status.getText(workDir));
            return null;
        }
        RecentItems.addRecentItem(logger, workDir.dir());
        return new ProjectFrame(logger, tools, rootDir, workDir, reopenStart);
    }

    private ProjectFrame(ErrorLogger logger, Tools tools, Path rootDir, Workdir workDir, boolean reopenStart) {
        super(logger, "KaraEd");
        this.workDir = workDir;
        this.runner = new ToolRunner(tools, rootDir, taLog::append);
        this.afterClose = () -> {
            if (reopenStart) {
                new StartFrame(logger, tools, rootDir);
            }
        };

        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toolBar.add(new JButton(new AbstractAction("Options") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OptionsDialog dlg = OptionsDialog.options(logger, ProjectFrame.this, workDir);
                    if (dlg.isSaved()) {
                        refreshStepStates();
                    }
                } catch (Exception ex) {
                    error(ex);
                }
            }
        }));
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(new JButton(runAction));
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(new JButton(stopAction));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(createToolsButton(this, tools));
        add(toolBar, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        add(main, BorderLayout.CENTER);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JTextField tfPath = new JTextField(40);
        tfPath.setEditable(false);
        InputUtil.setText(tfPath, workDir.dir().toAbsolutePath().normalize().toString());

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
            if (link == LinkType.BACKVOCALS) {
                editBackvocals(false);
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
                error("File not found");
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
                StepRunner stepRunner = new StepRunner(
                    workDir, runner, this::showTitle,
                    this::editRanges, this::editBackvocals
                );
                long t0 = System.currentTimeMillis();
                boolean ok = true;
                for (PipeStep step : PipeStep.values()) {
                    StepState state = pipe.stepStates().get(step);
                    // todo: always run ranges step??? or only if has some misalignment???
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
                                SwingUtilities.invokeLater(() -> error(ex.getMessage()));
                            } else {
                                message = ex.toString();
                                SwingUtilities.invokeLater(() -> error(ex));
                            }
                            setState(step, new RunStepState.Error(message));
                            runner.println("ERROR: " + message);
                            if (ex instanceof CommandException cmex) {
                                runner.println("Command line was:");
                                String commandLine = cmex.commandLine
                                    .stream()
                                    .map(str -> str.indexOf(' ') >= 0 ? '"' + str + '"' : str)
                                    .collect(Collectors.joining(" "));
                                runner.println(commandLine);
                            }
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
        if (running) {
            stopAction.setEnabled(true);
            runAction.setEnabled(false);
        } else {
            runAction.setEnabled(true);
            stopAction.setEnabled(false);
        }
    }

    private void editRanges() throws UnsupportedAudioFileException, IOException {
        if (!editRanges(true))
            throw new CancelledException();
    }

    private boolean editRanges(boolean canContinue) throws UnsupportedAudioFileException, IOException {
        Path ranges = workDir.file("ranges.json");
        Path lang = workDir.file("lang.json");
        Path vocals = workDir.vocals();
        Path text = workDir.file("text.txt");
        ManualAlign ma = ManualAlign.create(this, getLogger(), canContinue, vocals, text, ranges, lang);
        ma.setVisible(true);
        return ma.isContinue();
    }

    private void editBackvocals() throws UnsupportedAudioFileException, IOException {
        // todo: open only if non-empty???
        if (!editBackvocals(true))
            throw new CancelledException();
    }

    private boolean editBackvocals(boolean canContinue) throws UnsupportedAudioFileException, IOException {
        Path ranges = workDir.file("ranges.json");
        Path vocals = workDir.vocals();
        Path backvocals = workDir.file("backvocals.json");
        EditBackvocals ebv = EditBackvocals.create(this, getLogger(), canContinue, vocals, ranges, backvocals);
        ebv.setVisible(true);
        return ebv.isContinue();
    }

    @Override
    public boolean onClosing() {
        if (!runAction.isEnabled())
            return false;
        afterClose.run();
        return true;
    }
}
