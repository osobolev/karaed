package karaed.gui.project;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.engine.formats.ranges.Range;
import karaed.gui.AppContext;
import karaed.gui.ErrorLogger;
import karaed.gui.align.ManualAlign;
import karaed.gui.backvocals.EditBackvocals;
import karaed.gui.components.toolbar.ToolButtons;
import karaed.gui.options.OptionsDialog;
import karaed.gui.start.DirStatus;
import karaed.gui.start.RecentItems;
import karaed.gui.util.BaseFrame;
import karaed.gui.util.InputUtil;
import karaed.gui.util.LogArea;
import karaed.gui.util.TitleUtil;
import karaed.project.*;
import karaed.tools.CommandException;
import karaed.tools.ToolRunner;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
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

    private final AppContext ctx;
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

    public static ProjectFrame create(AppContext ctx, Workdir workDir,
                                      Consumer<String> onError, Runnable afterClose) {
        DirStatus status = DirStatus.test(workDir);
        if (status != DirStatus.OK) {
            onError.accept(status.getText(workDir));
            return null;
        }
        ErrorLogger projectLogger = ctx.mainLogger().derive(workDir.dir());
        RecentItems.addRecentItem(projectLogger, workDir.dir());
        return new ProjectFrame(projectLogger, ctx, workDir, afterClose);
    }

    private ProjectFrame(ErrorLogger projectLogger, AppContext ctx, Workdir workDir, Runnable afterClose) {
        super(projectLogger, "KaraEd");
        this.ctx = ctx;
        this.workDir = workDir;
        this.runner = new ToolRunner(ctx.tools(), taLog::append);
        this.afterClose = afterClose;

        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toolBar.add(new JButton(new AbstractAction("Options") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OptionsDialog dlg = OptionsDialog.options(projectLogger, ctx.tools(), ProjectFrame.this, workDir);
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
        toolBar.add(ToolButtons.create(this, ctx.tools()));
        add(toolBar, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout());
        add(main, BorderLayout.CENTER);

        JTextField tfPath = new JTextField(40);
        tfPath.setEditable(false);
        InputUtil.setText(tfPath, workDir.dir().toAbsolutePath().normalize().toString());
        JButton btnShowDir = InputUtil.getChooseButtonFor(
            tfPath, "...",
            () -> {
                try {
                    Desktop.getDesktop().open(workDir.dir().toFile());
                } catch (Exception ex) {
                    error(ex);
                }
            }
        );
        btnShowDir.setToolTipText("Open folder");

        tfTitle.setEditable(false);
        showTitle();

        JPanel top = new JPanel(new GridBagLayout());
        top.add(tfPath, new GridBagConstraints(
            0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));
        top.add(btnShowDir, new GridBagConstraints(
            1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0
        ));
        top.add(tfTitle, new GridBagConstraints(
            0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0
        ));
        top.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
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
        projectLogger.setDefault();
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
                    workDir, runner, ctx.appDir(), this::showTitle,
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
                    SwingUtilities.invokeLater(this::showSuccessNotification);
                }
            } finally {
                runThread = null;
                SwingUtilities.invokeLater(() -> enableDisable(false));
            }
        }, "KaraEd pipe");
        runThread = thread;
        thread.start();
    }

    private void showSuccessNotification() {
        if (!SystemTray.isSupported())
            return;
        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon[] trayIcons = tray.getTrayIcons();
        TrayIcon trayIcon;
        if (trayIcons.length > 0) {
            trayIcon = trayIcons[0];
        } else {
            Image image = getToolkit().getImage(ProjectFrame.class.getResource("/karaed.png"));
            trayIcon = new TrayIcon(image, "KaraEd");
            try {
                tray.add(trayIcon);
            } catch (Exception ex) {
                return;
            }
        }
        trayIcon.displayMessage("SUCCESS", "Karaoke created successfully", TrayIcon.MessageType.INFO);
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
        return ManualAlign.manualAlign(this, getLogger(), canContinue, vocals, text, ranges, lang);
    }

    private void editBackvocals() throws UnsupportedAudioFileException, IOException {
        if (!editBackvocals(true))
            throw new CancelledException();
    }

    private boolean editBackvocals(boolean canContinue) throws UnsupportedAudioFileException, IOException {
        EditBackvocals.Prepare prepare = EditBackvocals.prepare(workDir);
        return prepare.editBackvocals(this, getLogger(), canContinue);
    }

    private static void hideNotifications() {
        if (!SystemTray.isSupported())
            return;
        SystemTray tray = SystemTray.getSystemTray();
        TrayIcon[] trayIcons = tray.getTrayIcons();
        for (TrayIcon trayIcon : trayIcons) {
            tray.remove(trayIcon);
        }
    }

    @Override
    public boolean onClosing() {
        if (!runAction.isEnabled())
            return false;
        afterClose.run();
        hideNotifications();
        ErrorLogger mainLogger = ctx.mainLogger();
        mainLogger.setDefault();
        ErrorLogger projectLogger = getLogger();
        if (projectLogger != mainLogger) {
            projectLogger.close();
        }
        return true;
    }
}
