package karaed.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import karaed.ErrorLogger;
import karaed.FileAudioSource;
import karaed.FileLogger;
import karaed.gui.save.SaveData;
import karaed.model.AudioSource;
import karaed.model.MaxAudioSource;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class KaraGui {

    private final ErrorLogger logger;
    private final ColorSequence colors = new ColorSequence();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final RangesComponent vocals;
    private final JSlider scaleSlider = new JSlider(2, 50, 30);
    private final JPanel main = new JPanel(new BorderLayout());
    private final LyricsComponent lyrics = new LyricsComponent(colors);

    private final Action actionNew = new AbstractAction("New") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (checkSaved()) {
                newProject();
            }
        }
    };
    private final Action actionLoad = new AbstractAction("Load") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (checkSaved()) {
                load();
            }
        }
    };
    private final Action actionSave = new AbstractAction("Save") {
        @Override
        public void actionPerformed(ActionEvent e) {
            save();
        }
    };
    private final Action actionStop;

    private String vocalsPath = null;
    private List<String> origLines = null;
    private Path projectFile = null;

    KaraGui(ErrorLogger logger) {
        this.logger = logger;
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

        actionSave.setEnabled(false);

        JToolBar toolBar = new JToolBar();
        toolBar.add(actionNew);
        toolBar.add(actionLoad);
        toolBar.add(actionSave);
        toolBar.addSeparator();
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

        vocals.addRangesChanged(() -> {
            actionSave.setEnabled(true);
            syncNumbers();
            lyrics.recolor();
        });
        lyrics.addLinesChanged(() -> {
            actionSave.setEnabled(true);
            syncNumbers();
            vocals.recolor();
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

    JComponent getVisual() {
        return main;
    }

    private void setData(Path projectFile, MaxAudioSource maxSource, SaveData data) {
        this.projectFile = projectFile;
        this.vocalsPath = data.vocalsPath();
        this.origLines = data.origLines();

        vocals.setData(maxSource, data.ranges());
        lyrics.setLines(data.editedLines());

        syncNumbers();
        vocals.recolor();
        lyrics.recolor();
    }

    private void newProject() {
        Window owner = SwingUtilities.getWindowAncestor(main);
        NewProjectDialog dlg = new NewProjectDialog(owner);
        MaxAudioSource maxSource = dlg.getMaxSource();
        SaveData data = dlg.getData();
        if (maxSource == null || data == null)
            return;
        setData(null, maxSource, data);
        actionSave.setEnabled(true);
    }

    private void save() {
        if (projectFile == null) {
            JFileChooser chooser = new JFileChooser(new File("."));
            int ans = chooser.showSaveDialog(main);
            if (ans != JFileChooser.APPROVE_OPTION)
                return;
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile == null)
                return;
            projectFile = selectedFile.toPath();
        }
        SaveData data = new SaveData(vocalsPath, vocals.getRanges(), origLines, lyrics.getLines());
        try (BufferedWriter out = Files.newBufferedWriter(projectFile)) {
            gson.toJson(data, out);
        } catch (Exception ex) {
            logger.error(ex);
            ShowMessage.error(main, ex);
        }
        actionSave.setEnabled(false);
    }

    private void load() {
        JFileChooser chooser = new JFileChooser(new File("."));
        int ans = chooser.showOpenDialog(main);
        if (ans != JFileChooser.APPROVE_OPTION)
            return;
        File selectedFile = chooser.getSelectedFile();
        if (selectedFile == null)
            return;
        SaveData data;
        try (BufferedReader in = Files.newBufferedReader(selectedFile.toPath())) {
            data = gson.fromJson(in, SaveData.class);
        } catch (Exception ex) {
            logger.error(ex);
            ShowMessage.error(main, ex);
            return;
        }
        AudioSource source = new FileAudioSource(new File(data.vocalsPath()));
        MaxAudioSource maxSource;
        try {
            maxSource = MaxAudioSource.detectMaxValues(source);
        } catch (Exception ex) {
            logger.error(ex);
            ShowMessage.error(main, ex);
            return;
        }
        setData(selectedFile.toPath(), maxSource, data);
    }

    boolean checkSaved() {
        while (true) {
            if (!actionSave.isEnabled())
                return true;
            Boolean ans = ShowMessage.confirm3(main, "Save project data?");
            if (ans == null) {
                // Cancel, do not close application
                return false;
            }
            if (!ans.booleanValue()) {
                // No, do not save
                return true;
            }
            // Yes, try to save
            save();
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("KaraEd");

        KaraGui gui = new KaraGui(new FileLogger("karaed.log"));
        frame.add(gui.getVisual(), BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (gui.checkSaved()) {
                    frame.dispose();
                }
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
