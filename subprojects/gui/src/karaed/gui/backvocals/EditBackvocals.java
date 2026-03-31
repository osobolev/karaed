package karaed.gui.backvocals;

import karaed.engine.formats.backvocals.BackRange;
import karaed.engine.formats.backvocals.Backvocals;
import karaed.gui.ErrorLogger;
import karaed.gui.components.EditorButtons;
import karaed.gui.components.MusicAndLyrics;
import karaed.gui.components.model.BackvocalRanges;
import karaed.gui.components.model.EditableRanges;
import karaed.gui.components.model.RangesAndLyrics;
import karaed.gui.util.BaseFrame;
import karaed.gui.util.TouchUtil;
import karaed.json.JsonUtil;
import karaed.project.Workdir;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class EditBackvocals extends BaseFrame {

    private record DataFiles(
        Path backvocals,
        Path ranges,
        Path vocals
    ) {

        void touchBackvocalsIfSourceNewer() throws IOException {
            TouchUtil.touchIfSourceNewer(backvocals, ranges, vocals);
        }
    }

    private final DataFiles files;

    private final MusicAndLyrics<BackvocalsComponent> ml;

    private final EditorButtons butt;

    private final Action actionSave = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {
            save();
        }
    };

    private EditBackvocals(ErrorLogger logger, boolean canContinue,
                           EditableRanges model, List<String> lines,
                           DataFiles files, BackvocalRanges ranges, boolean fromFile) {
        super(logger, "Backvocals");
        this.files = files;

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

    private static Backvocals maybeLoad(Path backvocalsFile) throws IOException {
        return JsonUtil.readFile(backvocalsFile, Backvocals.class, () -> null);
    }

    public static final class Prepare {

        private final DataFiles files;
        private final Backvocals maybeData;

        Prepare(DataFiles files, Backvocals maybeData) {
            this.files = files;
            this.maybeData = maybeData;
        }

        private boolean hasData() {
            return maybeData != null && !maybeData.ranges().isEmpty();
        }

        private EditBackvocals create(ErrorLogger logger, boolean canContinue) throws UnsupportedAudioFileException, IOException {
            RangesAndLyrics rl = RangesAndLyrics.load(files.vocals, files.ranges, Collections.emptyList());
            List<BackRange> backRanges = maybeData == null ? Collections.emptyList() : maybeData.ranges();
            BackvocalRanges ranges = BackvocalRanges.convert(backRanges, rl.ranges().source.frameRate());
            return new EditBackvocals(
                logger, canContinue,
                rl.ranges(), rl.rangeLines(),
                files, ranges, maybeData != null
            );
        }

        public boolean editBackvocals(Window owner, ErrorLogger logger, boolean canContinue) throws UnsupportedAudioFileException, IOException {
            if (canContinue && !hasData()) {
                files.touchBackvocalsIfSourceNewer();
                return true;
            }
            EditBackvocals ebv = create(logger, canContinue);
            ebv.showModal(owner);
            return ebv.isContinue();
        }
    }

    public static Prepare prepare(Workdir workDir) throws IOException {
        Path backvocals = workDir.file("backvocals.json");
        Path ranges = workDir.file("ranges.json");
        Path vocals = workDir.vocals();
        Backvocals maybeData = maybeLoad(backvocals);
        return new Prepare(new DataFiles(backvocals, ranges, vocals), maybeData);
    }

    private boolean save() {
        Backvocals newData = ml.music.getData();
        boolean ok = false;
        if (actionSave.isEnabled()) {
            try {
                Backvocals currData = maybeLoad(files.backvocals);
                if (currData == null || !Objects.equals(newData.ranges(), currData.ranges())) {
                    JsonUtil.writeFile(files.backvocals, newData);
                } else {
                    files.touchBackvocalsIfSourceNewer();
                }

                actionSave.setEnabled(false);
                ok = true;
            } catch (Exception ex) {
                error(ex);
            }
        } else {
            try {
                files.touchBackvocalsIfSourceNewer();
                ok = true;
            } catch (IOException ex) {
                error(ex);
            }
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
