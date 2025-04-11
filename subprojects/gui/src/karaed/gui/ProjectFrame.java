package karaed.gui;

import karaed.engine.opts.OInput;
import karaed.gui.util.ShowMessage;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.swing.*;
import java.awt.BorderLayout;
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

    private final Workdir workDir;
    private final ProcRunner runner;
    private final LogArea taLog = new LogArea();

    public ProjectFrame(Tools tools, Path rootDir, Workdir workDir) {
        super("KaraEd");
        this.workDir = workDir;
        this.runner = new ProcRunner(tools, rootDir, taLog::append);

        JTextField tfPath = new JTextField(30);
        tfPath.setEditable(false);
        tfPath.setText(workDir.dir().toString());
        add(tfPath, BorderLayout.NORTH);

        add(new JScrollPane(taLog.getVisual()), BorderLayout.CENTER);

        JButton btnRun = new JButton("Run");
        btnRun.addActionListener(e -> {
            btnRun.setEnabled(false);
            Thread thread = new Thread(() -> {
                try {
                    getAudio();
                    demucs();
                    ranges();
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        if (ex instanceof KaraException) {
                            ShowMessage.error(this, ex.getMessage());
                        } else {
                            // todo: log it
                            ShowMessage.error(this, ex);
                        }
                    });
                } finally {
                    btnRun.setEnabled(true);
                }
            });
            thread.start();
        });
        JPanel butt = new JPanel();
        butt.add(btnRun);
        add(btnRun, BorderLayout.SOUTH);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void getAudio() throws IOException, InterruptedException {
        Path audio = workDir.audio();
        if (Files.exists(audio)) // todo: check input.json + options/cut.json
            return;
        OInput input = JsonUtil.readFile(workDir.file("input.json"), OInput.class);
        if (input.url() != null) {
            runner.runPythonExe(
                "yt-dlp",
                "--write-info-json", "-k",
                "--extract-audio",
                "--audio-format", "mp3",
                "--output", audio.toString(),
                input.url()
            );
            // todo: possibly cut audio/video
        } else if (input.file() != null) {
            Files.copy(Path.of(input.file()), audio);
            // todo: possibly cut audio
        } else {
            // todo: error
            return;
        }
    }

    private void demucs() throws IOException, InterruptedException {
        Path vocals = workDir.demuxed("vocals.wav");
        Path noVocals = workDir.demuxed("no_vocals.wav");
        if (Files.exists(vocals) && Files.exists(noVocals)) // todo: check audio.mp3 + options/demucs.json
            return;
        Path audio = workDir.audio();
        runner.runPythonExe(
            "demucs",
            "--two-stems=vocals",
            "--shifts=" + 1, // todo: get from options/demucs.json
            "--out=" + workDir.dir(),
            audio.toString()
        );
    }

    private void ranges() throws InterruptedException, InvocationTargetException {
        Path ranges = workDir.demuxed("ranges.json");
        if (Files.exists(ranges)) // todo: check text.txt + vocals.wav + options/ranges.json
            return;
        Runnable editRanges = () -> {
            // todo
        };
        if (SwingUtilities.isEventDispatchThread()) {
            editRanges.run();
        } else {
            SwingUtilities.invokeAndWait(editRanges);
        }
    }

    private void align() {
        // todo
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
