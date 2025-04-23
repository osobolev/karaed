package karaed.gui.align;

import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.ErrorLogger;
import karaed.gui.align.model.EditableRanges;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;

final class AlignComponent {

    private static final Icon ICON_STOP = InputUtil.getIcon("/stop.png");

    private final ColorSequence colors = new ColorSequence();
    private final EditableRanges model;

    private final RangesComponent vocals;
    private final JSlider scaleSlider = new JSlider(2, 50, 30);
    private final JSpinner chThreshold = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final float maxSilenceGap; // todo: editable
    private final float minRangeDuration; // todo: editable
    private final JPanel main = new JPanel(new BorderLayout());
    private final LyricsComponent lyrics = new LyricsComponent(colors);

    private final Action actionStop;

    AlignComponent(ErrorLogger logger, EditableRanges model, List<String> lines, Runnable onChange) {
        this.model = model;
        this.vocals = new RangesComponent(logger, colors, model);
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

        {
            AreaParams params = model.getParams();
            this.maxSilenceGap = params.maxSilenceGap();
            this.minRangeDuration = params.minRangeDuration();
            chThreshold.setValue(Math.round(params.silenceThreshold() * 100));
        }
        lyrics.setLines(lines);

        model.addListener(rangesChanged -> {
            onChange.run();
            if (rangesChanged) {
                syncNumbers();
                lyrics.recolor();
            }
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
            // todo: start edit, if not started (save data before start)
            // todo: disable area editing when editing params
            try {
                AreaParams params = getParams();
                model.splitByParams(params);
            } catch (Exception ex) {
                ShowMessage.error(main, logger, ex);
            }
        });
    }

    private void syncNumbers() {
        int n = Math.min(model.getRangeCount(), lyrics.getLineCount());
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
        AreaParams params = getParams(); // todo: get from model???
        return new Ranges(params, model.getRanges(), model.getAreas(), lyrics.getLines());
    }

    void close() {
        vocals.stop();
    }
}
