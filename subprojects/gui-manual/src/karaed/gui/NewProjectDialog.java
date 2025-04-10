package karaed.gui;

import karaed.FileAudioSource;
import karaed.VoiceRanges;
import karaed.gui.save.SaveData;
import karaed.model.AudioSource;
import karaed.model.MaxAudioSource;
import karaed.model.Range;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class NewProjectDialog extends JDialog {

    private final JTextField tfVocalsPath = new JTextField(40);
    private final JTextArea taLyrics = new JTextArea(30, 20);

    private MaxAudioSource maxSource = null;
    private SaveData data = null;

    NewProjectDialog(Window owner) {
        super(owner, "New project", ModalityType.DOCUMENT_MODAL);

        JButton btnChoose = InputUtil.getChooseButtonFor(tfVocalsPath, "...", () -> {
            JFileChooser chooser = new JFileChooser();
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("WAV file", "wav"));
            int ans = chooser.showOpenDialog(this);
            if (ans != JFileChooser.APPROVE_OPTION)
                return;
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile == null)
                return;
            tfVocalsPath.setText(selectedFile.getAbsolutePath());
        });

        JPanel top = new JPanel();
        top.add(new JLabel("Vocals .wav file:"));
        top.add(tfVocalsPath);
        top.add(btnChoose);

        JPanel center = new JPanel(new BorderLayout());
        JPanel lyricsLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        lyricsLabel.add(new JLabel("Lyrics:"));
        center.add(lyricsLabel, BorderLayout.NORTH);
        center.add(new JScrollPane(taLyrics), BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        JPanel butt = new JPanel();
        butt.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkInput()) {
                    dispose();
                }
            }
        }));
        butt.add(new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }));
        add(butt, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean checkInput() {
        String vocalsPath = tfVocalsPath.getText();
        if (vocalsPath.isEmpty()) {
            ShowMessage.error(this, "Please enter vocals path");
            return false;
        }
        Path path = Path.of(vocalsPath);
        if (!Files.isRegularFile(path)) {
            ShowMessage.error(this, "Please enter a valid vocals path");
            return false;
        }

        AudioSource source = new FileAudioSource(path.toFile());
        MaxAudioSource maxSource;
        List<Range> ranges;
        try {
            maxSource = MaxAudioSource.detectMaxValues(source);
            ranges = VoiceRanges.detectVoice(maxSource);
        } catch (Exception ex) {
            ShowMessage.error(this, ex);
            return false;
        }

        String text = taLyrics.getText().trim();
        if (text.isEmpty()) {
            ShowMessage.error(this, "Please enter lyrics");
            return false;
        }
        List<String> lines = text.lines().toList();

        this.maxSource = maxSource;
        this.data = new SaveData(vocalsPath, ranges, lines, lines);
        return true;
    }

    MaxAudioSource getMaxSource() {
        return maxSource;
    }

    SaveData getData() {
        return data;
    }
}
