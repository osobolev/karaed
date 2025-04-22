package karaed.gui.align;

import karaed.engine.audio.MaxAudioSource;
import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.ErrorLogger;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

final class AlignComponent {

    private static final Icon ICON_STOP = InputUtil.getIcon("/stop.png");

    private final ColorSequence colors = new ColorSequence();

    private final RangesComponent vocals;
    private final JSlider scaleSlider = new JSlider(2, 50, 30);
    private final JSpinner chThreshold = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final float maxSilenceGap; // todo: editable
    private final float minRangeDuration; // todo: editable
    private final JPanel main = new JPanel(new BorderLayout());
    private final LyricsComponent lyrics = new LyricsComponent(colors);

    private final Action actionStop;

    AlignComponent(ErrorLogger logger, MaxAudioSource maxSource, Ranges data, Runnable onChange) {
        this.vocals = new RangesComponent(logger, colors);
        this.actionStop = new AbstractAction("Stop", ICON_STOP) {
            @Override
            public void actionPerformed(ActionEvent e) {
                vocals.stop();
            }
        };
        this.maxSilenceGap = data.params().maxSilenceGap();
        this.minRangeDuration = data.params().minRangeDuration();

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

        chThreshold.setValue(Math.round(data.params().silenceThreshold() * 100));
        vocals.setData(maxSource, data.ranges(), data.areas());
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
                vocals.setParams(getParams());
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

    private AreaParams getParams() {
        return new AreaParams(getSilenceThreshold(), maxSilenceGap, minRangeDuration);
    }

    Document getRangesDocument() {
        return lyrics.getDocument();
    }

    JComponent getVisual() {
        return main;
    }

    Ranges getData() {
        AreaParams params = getParams();
        List<Area> areas = Collections.emptyList(); // todo!!!
        return new Ranges(params, vocals.getRanges(), areas, lyrics.getLines());
    }

    void close() {
        vocals.stop();
    }
}
