package karaed.gui.align;

import karaed.engine.audio.PreparedAudioSource;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Ranges;
import karaed.engine.steps.align.Align;
import karaed.gui.ErrorLogger;
import karaed.gui.align.lyrics.SyncLyrics;
import karaed.gui.align.model.EditableRanges;
import karaed.gui.util.BaseDialog;
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

// todo: undo/redo
public final class ManualAlign extends BaseDialog {

    private final Path rangesFile;
    private final Path textFile;
    private final Path langFile;

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
                        Path textFile, List<String> textLines,
                        Path langFile, String languageCode) {
        super(owner, logger, "Align vocals & lyrics");
        this.rangesFile = rangesFile;
        this.textFile = textFile;
        this.langFile = langFile;

        Runnable onChange = () -> actionSave.setEnabled(true);
        this.alignComponent = new AlignComponent(this, model, languageCode, rangeLines, onChange);
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
                                     Path vocals, Path textFile, Path rangesFile, Path langFile) throws IOException, UnsupportedAudioFileException {
        PreparedAudioSource maxSource = PreparedAudioSource.create(vocals.toFile());

        List<String> textLines = loadText(textFile);

        Ranges fileData = loadData(rangesFile);

        EditableRanges model;
        List<String> rangeLines;
        if (fileData != null) {
            model = new EditableRanges(maxSource, fileData.params(), fileData.ranges(), fileData.areas());
            rangeLines = fileData.lines();
        } else {
            AreaParams params = new AreaParams(1, 0.5f, 0.5f);
            model = new EditableRanges(maxSource, params, Collections.emptyList(), Collections.emptyList());
            model.splitByParams(null, params);
            rangeLines = textLines;
        }

        String languageCode = Align.readLanguage(langFile);

        return new ManualAlign(
            owner, logger, canContinue,
            rangesFile, model, rangeLines, fileData != null,
            textFile, textLines,
            langFile, languageCode
        );
    }

    private boolean save(boolean forceSync) {
        Ranges newData = alignComponent.getData();
        if (newData.rangeLines().size() != newData.ranges().size()) {
            if (forceSync) {
                tabs.setSelectedIndex(0);
                error("Please align vocals and lyrics");
                return false;
            } else {
                if (!confirm2("Vocals and lyrics are not aligned, really save?")) {
                    tabs.setSelectedIndex(0);
                    return false;
                }
            }
        }
        if (!syncComponent.isAligned()) {
            if (forceSync) {
                tabs.setSelectedIndex(1);
                error("Please match edited and original text");
                return false;
            } else {
                if (!confirm2("Edited and original texts are mismatched, really save?")) {
                    tabs.setSelectedIndex(1);
                    return false;
                }
            }
        }
        boolean ok = false;
        if (actionSave.isEnabled()) {
            List<String> newText = syncComponent.getText();
            String newLang = alignComponent.getLanguage();
            try {
                List<String> currText = loadText(textFile);
                if (!Objects.equals(newText, currText)) {
                    Files.write(textFile, newText);
                }

                Ranges currData = loadData(rangesFile);
                if (!Objects.equals(newData, currData)) {
                    JsonUtil.writeFile(rangesFile, newData);
                }

                String currLang = Align.readLanguage(langFile);
                if (!Objects.equals(newLang, currLang)) {
                    Align.writeLanguage(langFile, newLang);
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
        alignComponent.close();
        if (alignComponent.isSplitting()) {
            return confirm2("You have uncommitted changes. Really close?");
        }
        if (!actionSave.isEnabled())
            return true;
        return confirm2("You have unsaved changes. Really close?");
    }

    public boolean isContinue() {
        return isContinue;
    }
}
