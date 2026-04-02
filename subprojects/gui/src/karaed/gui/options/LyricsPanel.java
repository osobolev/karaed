package karaed.gui.options;

import karaed.engine.lyrics.LRCException;
import karaed.engine.lyrics.LRCLib;
import karaed.gui.components.toolbar.LinkLabel;
import karaed.gui.tools.SetupTools;
import karaed.gui.util.BaseWindow;
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

    private final BaseWindow owner;
    private final SetupTools tools;
    private final InputPanel input;
    private final JTextArea taLyrics = new JTextArea(22, 60);

    private static String readLyrics(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        return String.join("\n", lines);
    }

    LyricsPanel(OptCtx ctx, InputPanel input) throws IOException {
        super(null, () -> ctx.file("text.txt"), LyricsPanel::readLyrics, () -> "");
        this.owner = ctx.owner;
        this.tools = ctx.tools;
        this.input = input;

        taLyrics.setLineWrap(true);
        taLyrics.setWrapStyleWord(true);
        InputUtil.undoable(taLyrics);

        LinkLabel lblHead = LinkLabel.create(main, e -> loadLyrics());
        lblHead.setText(LinkLabel.labelText(null, "Lyrics: " + LinkLabel.linkText("#", "(load from LRClib)")));
        main.add(lblHead.getVisual(), new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 5, 5), 0, 0
        ));
        main.add(new JScrollPane(taLyrics), new GridBagConstraints(
            0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 5, 5), 0, 0
        ));

        main.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        setLyrics(origData);
    }

    private void setLyrics(String lyrics) {
        InputUtil.setText(taLyrics, lyrics);
    }

    @Override
    String newData() throws ValidationException {
        String text = taLyrics.getText();
        boolean hasText = text.lines().anyMatch(line -> !line.trim().isEmpty());
        if (!hasText) {
            throw new ValidationException("Input lyrics", taLyrics);
        }
        return text.lines().collect(Collectors.joining("\n"));
    }

    @Override
    void writeData(Path file, String data) throws IOException {
        Files.write(file, data.lines().toList());
    }

    private void loadLyrics() {
        new InputDetailsFetcher<String>(owner, tools, input).fetch(
            false,
            LRCLib::loadLyrics, this::setLyrics,
            ex -> ex instanceof LRCException
        );
    }
}
