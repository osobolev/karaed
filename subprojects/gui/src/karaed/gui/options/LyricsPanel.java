package karaed.gui.options;

import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

final class LyricsPanel extends BasePanel<String> {

    private final JTextArea taLyrics = new JTextArea(20, 60);

    private static String readLyrics(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        return String.join("\n", lines);
    }

    LyricsPanel(OptCtx ctx) throws IOException {
        super("Lyrics", () -> ctx.file("text.txt"), LyricsPanel::readLyrics, () -> "");

        taLyrics.setLineWrap(true);
        taLyrics.setWrapStyleWord(true);

        main.add(new JScrollPane(taLyrics), new GridBagConstraints(
            0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 5, 5), 0, 0
        ));

        InputUtil.setText(taLyrics, origData);
    }

    @Override
    String newData() {
        String text = taLyrics.getText();
        // todo: check is not empty
        return text.lines().collect(Collectors.joining("\n"));
    }

    @Override
    void writeData(Path file, String data) throws IOException {
        Files.write(file, data.lines().toList());
    }
}
