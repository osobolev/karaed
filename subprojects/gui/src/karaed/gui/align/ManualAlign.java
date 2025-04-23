package karaed.gui.align;

import karaed.engine.audio.AudioSource;
import karaed.engine.audio.MaxAudioSource;
import karaed.engine.audio.MemAudioSource;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.ErrorLogger;
import karaed.gui.align.model.EditableRanges;
import karaed.gui.util.CloseUtil;
import karaed.gui.util.ShowMessage;
import karaed.json.JsonUtil;

import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ManualAlign extends JDialog {

    private final ErrorLogger logger;
    private final Path rangesFile;
    private final Path textFile;

    private final AlignComponent alignComponent;
    private final SyncLyrics syncComponent;
    private final JTabbedPane tabs = new JTabbedPane();

    private final Action actionSave = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {
            save(false);
        }
    };

    private boolean isContinue = false;

    private ManualAlign(Window owner, ErrorLogger logger, boolean canContinue,
                        Path rangesFile, EditableRanges model, List<String> rangeLines, boolean fromFile,
                        Path textFile, List<String> textLines) {
        super(owner, "Align vocals & lyrics", ModalityType.APPLICATION_MODAL);
        this.logger = logger;
        this.rangesFile = rangesFile;
        this.textFile = textFile;

        Runnable onChange = () -> actionSave.setEnabled(true);
        this.alignComponent = new AlignComponent(logger, model, rangeLines, onChange);
        this.syncComponent = new SyncLyrics(alignComponent.getRangesDocument(), String.join("\n", textLines), onChange);

        actionSave.setEnabled(!fromFile);

        tabs.addTab("Align vocals & lyrics", alignComponent.getVisual());
        tabs.addTab("Sync changes with text", syncComponent.getVisual());
        add(tabs, BorderLayout.CENTER);

        JPanel butt = new JPanel();
        butt.add(new JButton(actionSave));
        if (canContinue) {
            butt.add(new JButton(new AbstractAction("Save & continue") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (save(true)) {
                        isContinue = true;
                        dispose();
                    }
                }
            }));
        }
        butt.add(new JButton(new AbstractAction(canContinue ? "Cancel" : "Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }));
        add(butt, BorderLayout.SOUTH);

        CloseUtil.listen(this, this::onClosing);
        pack();
        setLocationRelativeTo(null);
    }

    private static Ranges loadData(Path rangesFile) throws IOException {
        if (Files.exists(rangesFile)) {
            return JsonUtil.readFile(rangesFile, Ranges.class);
        }
        return null;
    }

    private static List<String> loadText(Path textFile) throws IOException {
        return Files.readAllLines(textFile);
    }

    public static ManualAlign create(Window owner, ErrorLogger logger, boolean canContinue,
                                     Path vocals, Path textFile, Path rangesFile) throws IOException, UnsupportedAudioFileException {
        AudioSource source = MemAudioSource.create(vocals.toFile());
        MaxAudioSource maxSource = MaxAudioSource.detectMaxValues(source);

        List<String> textLines = loadText(textFile);

        Ranges fileData = loadData(rangesFile);

        EditableRanges model;
        List<String> rangeLines;
        if (fileData != null) {
            model = new EditableRanges(maxSource, fileData.params(), fileData.ranges(), fileData.areas());
            rangeLines = fileData.lines();
        } else {
            AreaParams params = new AreaParams(0.01f, 0.5f, 0.5f); // todo!!!
            model = new EditableRanges(maxSource, params, Collections.emptyList(), Collections.emptyList());
            model.splitByParams(params);
            rangeLines = textLines;
        }

        return new ManualAlign(
            owner, logger, canContinue,
            rangesFile, model, rangeLines, fileData != null,
            textFile, textLines
        );
    }

    private boolean save(boolean forceSync) {
        Ranges newData = alignComponent.getData();
        if (newData.rangeLines().size() != newData.ranges().size()) {
            if (forceSync) {
                tabs.setSelectedIndex(0);
                ShowMessage.error(this, "Please align vocals and lyrics");
                return false;
            } else {
                if (!ShowMessage.confirm2(this, "Vocals and lyrics are not aligned, really save?")) {
                    tabs.setSelectedIndex(0);
                    return false;
                }
            }
        }
        if (!syncComponent.isAligned()) {
            if (forceSync) {
                tabs.setSelectedIndex(1);
                ShowMessage.error(this, "Please match edited and original text");
                return false;
            } else {
                if (!ShowMessage.confirm2(this, "Edited and original texts are mismatched, really save?")) {
                    tabs.setSelectedIndex(1);
                    return false;
                }
            }
        }
        boolean ok = false;
        if (actionSave.isEnabled()) {
            List<String> newText = syncComponent.getText();
            try {
                Ranges currData = loadData(rangesFile);
                if (!Objects.equals(newData, currData)) {
                    JsonUtil.writeFile(rangesFile, newData);
                }
                List<String> currText = loadText(textFile);
                if (!Objects.equals(newText, currText)) {
                    Files.write(textFile, newText);
                }
                actionSave.setEnabled(false);
                ok = true;
            } catch (Exception ex) {
                ShowMessage.error(this, logger, ex);
            }
        } else {
            ok = true;
        }
        return ok;
    }

    private boolean onClosing() {
        alignComponent.close();
        if (!actionSave.isEnabled())
            return true;
        return ShowMessage.confirm2(this, "You have unsaved changes. Really close?");
    }

    public boolean isContinue() {
        return isContinue;
    }
}
