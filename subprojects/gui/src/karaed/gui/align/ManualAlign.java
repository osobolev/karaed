package karaed.gui.align;

import karaed.engine.audio.AudioSource;
import karaed.engine.audio.FileAudioSource;
import karaed.engine.audio.MaxAudioSource;
import karaed.engine.audio.VoiceRanges;
import karaed.engine.formats.ranges.Range;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.ErrorLogger;
import karaed.gui.util.ShowMessage;
import karaed.json.JsonUtil;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ManualAlign extends JDialog{

    private final ErrorLogger logger;
    private final Path rangesFile;

    private final ColorSequence colors = new ColorSequence();

    private final RangesComponent vocals;
    private final JSlider scaleSlider = new JSlider(2, 50, 30);
    private final JPanel main = new JPanel(new BorderLayout());
    private final LyricsComponent lyrics = new LyricsComponent(colors);

    private final Action actionStop;

    private boolean changed = false;
    private boolean ok = false;

    private ManualAlign(Window owner, ErrorLogger logger, Path rangesFile, MaxAudioSource maxSource, Ranges data) {
        super(owner, "Align vocals & lyrics");
        this.logger = logger;
        this.rangesFile = rangesFile;

        this.vocals = new RangesComponent(logger, colors);
        this.actionStop = new AbstractAction("Stop") { // todo: change to icon
            @Override
            public void actionPerformed(ActionEvent e) {
                vocals.stop();
            }
        };

        setScale();
        scaleSlider.addChangeListener(e -> setScale());

        enableDisableStop();
        vocals.addPlayChanged(this::enableDisableStop);

        JToolBar toolBar = new JToolBar();
        toolBar.add(actionStop);
        toolBar.addSeparator();
        toolBar.add(new JLabel("Scale:"));
        toolBar.add(scaleSlider);

        JPanel top = new JPanel(new BorderLayout());
        top.add(toolBar, BorderLayout.NORTH);
        JScrollPane spv = new JScrollPane(vocals, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        top.add(spv, BorderLayout.CENTER);

        main.add(top, BorderLayout.NORTH);

        JScrollPane spl = new JScrollPane(lyrics.getVisual());
        spl.setPreferredSize(new Dimension(1000, 400));
        main.add(spl, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        JPanel butt = new JPanel();
        butt.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (save()) {
                    ok = true;
                    dispose();
                }
            }
        }));
        butt.add(new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }));
        add(butt, BorderLayout.SOUTH);

        vocals.addRangesChanged(() -> {
            changed = true;
            syncNumbers();
            lyrics.recolor();
        });
        lyrics.addLinesChanged(() -> {
            changed = true;
            syncNumbers();
            vocals.recolor();
        });

        vocals.setData(maxSource, data.ranges());
        lyrics.setLines(data.lines());

        syncNumbers();
        vocals.recolor();
        lyrics.recolor();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onClosing()) {
                    dispose();
                }
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    public static ManualAlign create(Window owner, ErrorLogger logger, Path vocals, Path text, Path rangesFile) throws IOException, UnsupportedAudioFileException {
        AudioSource source = new FileAudioSource(vocals.toFile());
        MaxAudioSource maxSource = MaxAudioSource.detectMaxValues(source);

        Ranges data;
        if (Files.exists(rangesFile)) {
            data = JsonUtil.readFile(rangesFile, Ranges.class);
        } else {
            List<Range> ranges = VoiceRanges.detectVoice(maxSource);
            List<String> lines = Files.readAllLines(text);
            data = new Ranges(ranges, lines);
        }

        return new ManualAlign(owner, logger, rangesFile, maxSource, data);
    }

    private void syncNumbers() {
        int n = Math.min(vocals.getRangeCount(), lyrics.getLineCount());
        colors.setNumber(n);
    }

    private void setScale() {
        vocals.setScale(scaleSlider.getValue());
    }

    private void enableDisableStop() {
        actionStop.setEnabled(vocals.isPlaying());
    }

    private boolean save() {
        Ranges data = new Ranges(vocals.getRanges(), lyrics.getLines());
        try {
            JsonUtil.writeFile(rangesFile, data);
            changed = false;
            return true;
        } catch (Exception ex) {
            ShowMessage.error(main, logger, ex);
        }
        return false;
    }

    private boolean onClosing() {
        if (!changed)
            return true;
        return ShowMessage.confirm2(this, "You have unsaved changes. Really close?");
    }

    public boolean isOK() {
        return ok;
    }
}
