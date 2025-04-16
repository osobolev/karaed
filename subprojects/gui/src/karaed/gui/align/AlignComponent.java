package karaed.gui.align;

import karaed.engine.audio.MaxAudioSource;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.ErrorLogger;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;
import karaed.json.JsonUtil;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;

final class AlignComponent {

    private static final Icon ICON_STOP = InputUtil.getIcon("/stop.png");

    private final ErrorLogger logger;
    private final Path rangesFile;

    private final ColorSequence colors = new ColorSequence();

    private final RangesComponent vocals;
    private final JSlider scaleSlider = new JSlider(2, 50, 30);
    private final JSpinner chThreshold = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final JPanel main = new JPanel(new BorderLayout());
    private final LyricsComponent lyrics = new LyricsComponent(colors);

    private final Action actionStop;

    AlignComponent(ErrorLogger logger, Path rangesFile, MaxAudioSource maxSource, Ranges data, Runnable onChange) {
        this.logger = logger;
        this.rangesFile = rangesFile;

        this.vocals = new RangesComponent(logger, colors);
        this.actionStop = new AbstractAction("Stop", ICON_STOP) {
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
        toolBar.add(new JButton(actionStop));
        toolBar.addSeparator();
        toolBar.add(new JLabel("Scale:"));
        toolBar.add(scaleSlider);
        toolBar.add(new JLabel("Silence threshold, %:"));
        toolBar.add(chThreshold);

        JPanel top = new JPanel(new BorderLayout());
        top.add(toolBar, BorderLayout.NORTH);
        JScrollPane spv = new JScrollPane(vocals, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        top.add(spv, BorderLayout.CENTER);

        main.add(top, BorderLayout.NORTH);

        main.add(lyrics.getVisual(), BorderLayout.CENTER);

        chThreshold.setValue(Math.round(data.silenceThreshold() * 100));
        vocals.setData(maxSource, data.ranges());
        lyrics.setLines(data.lines());

        vocals.addRangesChanged(() -> {
            onChange.run();
            syncNumbers();
            lyrics.recolor();
        });
        lyrics.addLinesChanged(() -> {
            onChange.run();
            syncNumbers();
            vocals.recolor();
        });

        syncNumbers();
        vocals.recolor();
        lyrics.recolor();

        chThreshold.addChangeListener(e -> {
            try {
                vocals.setSilenceThreshold(getSilenceThreshold());
            } catch (Exception ex) {
                ShowMessage.error(main, logger, ex);
            }
        });
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

    private float getSilenceThreshold() {
        Number threshold = (Number) chThreshold.getValue();
        return threshold.floatValue() / 100f;
    }

    JComponent getVisual() {
        return main;
    }

    boolean save() {
        Ranges data = new Ranges(getSilenceThreshold(), vocals.getRanges(), lyrics.getLines());
        try {
            JsonUtil.writeFile(rangesFile, data);
            return true;
        } catch (Exception ex) {
            ShowMessage.error(main, logger, ex);
        }
        return false;
    }

    void close() {
        vocals.stop();
    }
}
