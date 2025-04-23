package karaed.gui.align;

import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.Range;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

final class AlignComponent {

    private static final Icon ICON_STOP = InputUtil.getIcon("/stop.png");

    private final ColorSequence colors = new ColorSequence();
    private final EditableRanges model;
    private final Runnable onChange;

    private final RangesComponent vocals;
    private final JSlider scaleSlider = new JSlider(2, 50, 30);
    private final ParamsComponent paramsInput = new ParamsComponent();
    private final JButton btnCommit = new JButton("Commit");
    private final JButton btnRollback = new JButton("Rollback");
    private final JPanel main = new JPanel(new BorderLayout());
    private final LyricsComponent lyrics = new LyricsComponent(colors);

    private final Action actionStop;

    private boolean splitModified = false;

    AlignComponent(BaseWindow owner, EditableRanges model, List<String> lines, Runnable onChange) {
        this.model = model;
        this.onChange = onChange;

        this.vocals = new RangesComponent(owner, colors, model);
        this.actionStop = new AbstractAction("Stop", ICON_STOP) {
            @Override
            public void actionPerformed(ActionEvent e) {
                vocals.stop();
            }
        };

        setScale();
        scaleSlider.addChangeListener(e -> setScale());

        enableDisableStop();
        vocals.addPlayChangeListener(this::enableDisableStop);

        JToolBar toolBar = new JToolBar();
        toolBar.add(new JButton(actionStop));
        toolBar.addSeparator();
        toolBar.add(new JLabel("Scale:"));
        toolBar.add(scaleSlider);
        toolBar.add(paramsInput.getVisual());
        toolBar.add(btnCommit);
        toolBar.add(btnRollback);

        JPanel top = new JPanel(new BorderLayout());
        top.add(toolBar, BorderLayout.NORTH);
        JScrollPane spv = new JScrollPane(vocals, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        top.add(spv, BorderLayout.CENTER);

        main.add(top, BorderLayout.NORTH);

        main.add(lyrics.getVisual(), BorderLayout.CENTER);

        lyrics.setLines(lines);

        model.addListener(rangesChanged -> {
            if (vocals.isSplitting()) {
                splitModified = true;
            } else {
                onChange.run();
            }
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

        vocals.addParamListener(paramsInput::setParams);
        paramsInput.addListener(params -> {
            startSplitting();
            vocals.setParams(params);
        });
        vocals.fireParamsChanged();

        btnCommit.addActionListener(e -> endSplitting(true));
        btnRollback.addActionListener(e -> endSplitting(false));
        endSplitting(false);
    }

    private void startSplitting() {
        if (!vocals.isSplitting()) {
            vocals.startSplitting();
            splitModified = false;
            btnCommit.setEnabled(true);
            btnRollback.setEnabled(true);
        }
    }

    private void endSplitting(boolean commit) {
        if (commit) {
            if (splitModified) {
                onChange.run();
            }
        } else {
            vocals.rollbackChanges();
        }
        vocals.finishSplitting();
        btnCommit.setEnabled(false);
        btnRollback.setEnabled(false);
        splitModified = false;
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

    Document getRangesDocument() {
        return lyrics.getDocument();
    }

    JComponent getVisual() {
        return main;
    }

    Ranges getData() {
        List<Area> areas = new ArrayList<>();
        for (EditableArea area : model.getAreas()) {
            areas.add(new Area(area.from(), area.to(), area.params()));
        }
        List<Range> ranges = model.getRanges().stream().toList();
        return new Ranges(model.getParams(), ranges, areas, lyrics.getLines());
    }

    void close() {
        vocals.stop();
    }
}
