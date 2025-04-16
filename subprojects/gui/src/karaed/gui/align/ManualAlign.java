package karaed.gui.align;

import karaed.engine.audio.AudioSource;
import karaed.engine.audio.FileAudioSource;
import karaed.engine.audio.MaxAudioSource;
import karaed.engine.audio.VoiceRanges;
import karaed.engine.formats.ranges.Range;
import karaed.engine.formats.ranges.Ranges;
import karaed.gui.ErrorLogger;
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
import java.util.List;

public final class ManualAlign extends JDialog {

    private final AlignComponent alignComponent;

    private boolean changed = false;
    private boolean ok = false;

    private ManualAlign(Window owner, ErrorLogger logger, Path rangesFile, MaxAudioSource maxSource, Ranges data) {
        super(owner, "Align vocals & lyrics", ModalityType.APPLICATION_MODAL);

        this.alignComponent = new AlignComponent(logger, rangesFile, maxSource, data, () -> changed = true);

        add(alignComponent.getVisual(), BorderLayout.CENTER);

        JPanel butt = new JPanel();
        butt.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (save()) {
                    ok = true;
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

        CloseUtil.listen(this, this::onClosing);
        pack();
        setLocationRelativeTo(null);
    }

    public static ManualAlign create(Window owner, ErrorLogger logger, Path vocals, Path text, Path rangesFile) throws IOException, UnsupportedAudioFileException {
        AudioSource source = new FileAudioSource(vocals.toFile());
        MaxAudioSource maxSource = MaxAudioSource.detectMaxValues(source);

        Ranges data;
        if (Files.exists(rangesFile)) {
            data = JsonUtil.readFile(rangesFile, Ranges.class);
        } else {
            float silenceThreshold = 0.01f;
            List<Range> ranges = VoiceRanges.detectVoice(maxSource, silenceThreshold);
            List<String> lines = Files.readAllLines(text);
            data = new Ranges(silenceThreshold, ranges, lines);
        }

        return new ManualAlign(owner, logger, rangesFile, maxSource, data);
    }

    private boolean save() {
        boolean ok = alignComponent.save();
        if (ok) {
            changed = false;
        }
        return ok;
    }

    private boolean onClosing() {
        alignComponent.close();
        if (!changed)
            return true;
        return ShowMessage.confirm2(this, "You have unsaved changes. Really close?");
    }

    public boolean isOK() {
        return ok;
    }
}
