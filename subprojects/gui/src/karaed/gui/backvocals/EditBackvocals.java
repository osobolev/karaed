package karaed.gui.backvocals;

import karaed.engine.formats.backvocals.Backvocals;
import karaed.gui.ErrorLogger;
import karaed.gui.components.EditorButtons;
import karaed.gui.components.MusicAndLyrics;
import karaed.gui.components.model.BackvocalRanges;
import karaed.gui.components.model.EditableRanges;
import karaed.gui.components.model.RangesAndLyrics;
import karaed.gui.util.BaseDialog;
import karaed.json.JsonUtil;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EditBackvocals extends BaseDialog {

    private final Path backvocalsFile;

    private final MusicAndLyrics<BackvocalsComponent> ml;

    private final EditorButtons butt;

    private final Action actionSave = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {
            save();
        }
    };

    private EditBackvocals(Window owner, ErrorLogger logger, boolean canContinue,
                           EditableRanges model, List<String> lines,
                           Path backvocalsFile, BackvocalRanges ranges, boolean fromFile) {
        super(owner, logger, "Backvocals");
        this.backvocalsFile = backvocalsFile;

        Runnable onChange = () -> actionSave.setEnabled(true);
        this.ml = new MusicAndLyrics<>(
            model, lines, false,
            colors -> new BackvocalsComponent(this, colors, model, ranges, onChange)
        );

        actionSave.setEnabled(!fromFile);

        JPanel toolBar = new JPanel(new GridBagLayout());
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        toolBar.add(new JButton(ml.actionStop), new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0
        ));
        toolBar.add(new JLabel("Scale:"), new GridBagConstraints(
            1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 15, 0, 0), 0, 0
        ));
        toolBar.add(ml.scaleSlider, new GridBagConstraints(
            2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 0), 0, 0
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.add(toolBar, BorderLayout.NORTH);
        top.add(ml.wrapMusic(), BorderLayout.CENTER);

        JPanel main = new JPanel(new BorderLayout());
        main.add(top, BorderLayout.NORTH);
        main.add(ml.lyrics.getVisual(), BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);

        this.butt = new EditorButtons(this, canContinue, actionSave, this::save);
        add(butt.getVisual(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public static EditBackvocals create(Window owner, ErrorLogger logger, boolean canContinue,
                                        Path vocals, Path rangesFile, Path backvocalsFile) throws UnsupportedAudioFileException, IOException {
        RangesAndLyrics rl = RangesAndLyrics.load(vocals, rangesFile, Collections.emptyList());
        Backvocals fileData = BackvocalRanges.loadRaw(backvocalsFile);
        BackvocalRanges ranges = BackvocalRanges.convert(fileData, rl.ranges().source.frameRate());
        return new EditBackvocals(
            owner, logger, canContinue,
            rl.ranges(), rl.rangeLines(),
            backvocalsFile, ranges, fileData.manual()
        );
    }

    private boolean save() {
        Backvocals newData = ml.music.getData();
        boolean ok = false;
        if (actionSave.isEnabled()) {
            try {
                Backvocals currData = BackvocalRanges.loadRaw(backvocalsFile);
                if (!Objects.equals(newData.ranges(), currData.ranges())) {
                    JsonUtil.writeFile(backvocalsFile, newData);
                }

                actionSave.setEnabled(false);
                ok = true;
            } catch (Exception ex) {
                error(ex);
            }
        } else {
            ok = true;
        }
        return ok;
    }

    @Override
    public boolean onClosing() {
        ml.music.stop();
        if (!actionSave.isEnabled())
            return true;
        return confirm2("You have unsaved changes. Really close?");
    }

    public boolean isContinue() {
        return butt.isContinue();
    }
}
