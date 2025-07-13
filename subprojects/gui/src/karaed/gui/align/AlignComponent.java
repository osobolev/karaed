package karaed.gui.align;

import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.Range;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.align.lyrics.LyricsComponent;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;
import karaed.gui.align.model.Language;
import karaed.gui.align.vocals.RangesComponent;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;

final class AlignComponent {

    private static final Icon ICON_STOP = InputUtil.getIcon("/stop.png");

    private final ColorSequence colors = new ColorSequence();
    private final EditableRanges model;
    private final Runnable onChange;

    private final RangesComponent vocals;
    private final JComboBox<Language> chLanguage = new JComboBox<>(Language.languages());
    private final JSlider scaleSlider = new JSlider(2, 50, 30);
    private final JButton btnResplit = new JButton();
    private final ParamsComponent paramsInput = new ParamsComponent();
    private final JButton btnCommit = new JButton("Commit");
    private final JButton btnRollback = new JButton("Rollback");
    private final JPanel main = new JPanel(new BorderLayout());
    private final LyricsComponent lyrics;

    private final Action actionStop;

    private boolean splitModified = false;

    AlignComponent(BaseWindow owner, EditableRanges model, String languageCode, List<String> lines, Runnable onChange) {
        this.model = model;
        this.onChange = onChange;

        this.lyrics = new LyricsComponent(colors);
        this.vocals = new RangesComponent(
            owner, colors, model,
            this::endSplitting, lyrics::getLineAt
        );
        lyrics.addLyricsListener(vocals::showRange);
        vocals.addGoToListener(lyrics::goTo);

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

        JPanel toolBar = new JPanel(new GridBagLayout());
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        toolBar.add(new JButton(actionStop), new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0
        ));
        toolBar.add(new JLabel("Language:"), new GridBagConstraints(
            1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 15, 0, 0), 0, 0
        ));
        toolBar.add(chLanguage, new GridBagConstraints(
            2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0
        ));
        toolBar.add(new JLabel("Scale:"), new GridBagConstraints(
            3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 15, 0, 0), 0, 0
        ));
        toolBar.add(scaleSlider, new GridBagConstraints(
            4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0
        ));

        JPanel params = new JPanel(new FlowLayout(FlowLayout.LEFT));
        params.add(btnResplit);
        params.add(paramsInput.getVisual());
        params.add(btnCommit);
        params.add(btnRollback);

        JPanel pranges = new JPanel(new BorderLayout());
        pranges.add(params, BorderLayout.NORTH);
        JScrollPane spv = new JScrollPane(vocals, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pranges.add(spv, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.add(toolBar, BorderLayout.NORTH);
        top.add(pranges, BorderLayout.CENTER);

        main.add(top, BorderLayout.NORTH);
        main.add(lyrics.getVisual(), BorderLayout.CENTER);

        chLanguage.setSelectedItem(Language.valueOf(languageCode));
        chLanguage.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                onChange.run();
            }
        });

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
        paramsInput.addListener(vocals::setParams);
        vocals.fireParamsChanged();

        vocals.addAreaListener(forEdit -> {
            showEditLabel();
            if (forEdit) {
                startSplitting();
            }
        });
        showEditLabel();
        btnResplit.addActionListener(e -> startSplitting());
        btnCommit.addActionListener(e -> endSplitting(true));
        btnRollback.addActionListener(e -> endSplitting(false));
        endSplitting(false);
    }

    private void showEditLabel() {
        btnResplit.setText(vocals.isAreaSelected() ? "Edit area params" : "Edit global params");
    }

    private void startSplitting() {
        if (!vocals.isSplitting()) {
            vocals.startSplitting();
            splitModified = false;
            btnResplit.setEnabled(false);
            paramsInput.setEnabled(true);
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
            syncNumbers();
            lyrics.recolor();
        }
        vocals.finishSplitting();
        btnResplit.setEnabled(true);
        paramsInput.setEnabled(false);
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

    String getLanguage() {
        Language language = (Language) chLanguage.getSelectedItem();
        assert language != null;
        return language.code();
    }

    Ranges getData() {
        List<Area> areas = new ArrayList<>();
        for (EditableArea area : model.getAreas()) {
            areas.add(new Area(area.from(), area.to(), area.params()));
        }
        List<Range> ranges = model.getRanges().stream().toList();
        return new Ranges(model.getParams(), ranges, areas, lyrics.getLines());
    }

    boolean isSplitting() {
        return vocals.isSplitting();
    }

    void close() {
        vocals.stop();
    }
}
