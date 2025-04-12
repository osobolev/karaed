package karaed.gui.project;

import karaed.engine.KaraException;
import karaed.engine.opts.ODemucs;
import karaed.engine.opts.OInput;
import karaed.engine.steps.align.Align;
import karaed.engine.steps.demucs.Demucs;
import karaed.engine.steps.youtube.Youtube;
import karaed.gui.ErrorLogger;
import karaed.gui.align.ManualAlign;
import karaed.gui.options.OptionsDialog;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

// todo: options/cut.json - what piece of original file to use (from-to)
// todo: options/demucs.json - number of shifts
// todo: options/ranges.json - auto/manual
// todo: options/align.json - by words/chars
// todo: options/karaoke.json - karaoke generation properties
public final class ProjectFrame extends JFrame {

    private final ErrorLogger logger;
    private final Workdir workDir;
    private final ProcRunner runner;
    private final LogArea taLog = new LogArea();

    private final Action runAction = new AbstractAction("Run") {
        @Override
        public void actionPerformed(ActionEvent e) {
            runPipeline();
        }
    };

    public ProjectFrame(ErrorLogger logger, Tools tools, Path rootDir, Workdir workDir) {
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

        JTextField tfPath = new JTextField(30);
        tfPath.setEditable(false);
        InputUtil.setText(tfPath, workDir.dir().toString());
        main.add(tfPath, BorderLayout.NORTH);
        // todo: display from info.json

        // todo: display performed steps:
        main.add(new JLabel("Info"), BorderLayout.CENTER);

        main.add(new JScrollPane(taLog.getVisual()), BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void runPipeline() {
        runAction.setEnabled(false);
        Thread thread = new Thread(() -> {
            try {
                getAudio();
                demucs();
                ranges();
                align();
            } catch (Throwable ex) {
                if (ex instanceof KaraException) {
                    SwingUtilities.invokeLater(() -> ShowMessage.error(this, ex.getMessage()));
                } else if (!(ex instanceof CancelledException)) {
                    SwingUtilities.invokeLater(() -> ShowMessage.error(this, logger, ex));
                }
            } finally {
                SwingUtilities.invokeLater(() -> runAction.setEnabled(true));
            }
        });
        thread.start();
    }

    private void getAudio() throws IOException, InterruptedException {
        Path audio = workDir.audio();
        if (Files.exists(audio)) // todo: check input.json + options/cut.json
            return;
        OInput input = JsonUtil.readFile(workDir.file("input.json"), OInput.class);
        Youtube.download(runner, input, audio);
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
