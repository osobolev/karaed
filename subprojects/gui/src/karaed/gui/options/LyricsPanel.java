package karaed.gui.options;

import karaed.engine.lyrics.LRCException;
import karaed.engine.lyrics.LRCLib;
import karaed.gui.components.toolbar.LinkLabel;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

final class LyricsPanel extends BasePanel<String> {

    private static final Color WARN_COLOR = new Color(255, 120, 120);

    private final InputPanel input;
    private final JTextArea taLyrics = new JTextArea(22, 60) {
        @Override
        public String getToolTipText(MouseEvent event) {
            int i = viewToModel2D(event.getPoint());
            if (i < 0)
                return null;
            SequencedCollection<String> warnings = getWarnings(i);
            if (warnings == null)
                return null;
            if (warnings.size() == 1)
                return warnings.getFirst();
            return "<html>" + String.join("<br>\n", warnings) + "</html>";
        }
    };

    private NavigableMap<Integer, BadRange> badRanges = Collections.emptyNavigableMap();

    private static String readLyrics(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file);
        return String.join("\n", lines);
    }

    LyricsPanel(OptCtx ctx, InputPanel input) throws IOException {
        super(ctx, null, () -> ctx.file("text.txt"), LyricsPanel::readLyrics, () -> "");
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

        taLyrics.getDocument().addDocumentListener(new DocumentListener() {

            private void changed() {
                // todo: do it with delay???
                markSuspiciousSymbols();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                changed();
            }
        });
        markSuspiciousSymbols();
        ToolTipManager.sharedInstance().registerComponent(taLyrics);
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
        new InputDetailsFetcher<String>(ctx, input).fetch(
            false,
            LRCLib::loadLyrics, this::setLyrics,
            ex -> {
                if (ex instanceof LRCException lex) {
                    searchGoogle(lex);
                    return true;
                } else {
                    return false;
                }
            }
        );
    }

    private void searchGoogle(LRCException lex) {
        String title = lex.info.shortTitle();
        if (title == null) {
            ctx.owner.error(lex.getMessage());
            return;
        }
        int ans = JOptionPane.showConfirmDialog(
            ctx.owner.toWindow(), lex.getMessage() + "\nGoogle for lyrics?", "Error",
            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE
        );
        if (ans != JOptionPane.YES_OPTION)
            return;
        try {
            URI uri = URI.create("https://www.google.com/search?q=" + URLEncoder.encode(title + " lyrics", StandardCharsets.UTF_8));
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            ctx.owner.error(ex);
        }
    }

    private static final class BadRange {

        final int from;
        int to;
        final LinkedHashSet<String> warnings = new LinkedHashSet<>();

        BadRange(int index, String warning) {
            this.from = index;
            this.to = index;
            this.warnings.add(warning);
        }
    }

    private void markSuspiciousSymbols() {
        String text = taLyrics.getText();
        TreeMap<Integer, BadRange> badRanges = new TreeMap<>();
        checkSymbols(text, (i, warning) -> {
            if (!badRanges.isEmpty()) {
                BadRange last = badRanges.lastEntry().getValue();
                if (last.to + 1 == i) {
                    last.to = i;
                    last.warnings.add(warning);
                    return;
                }
            }
            badRanges.put(i, new BadRange(i, warning));
        });
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(WARN_COLOR);
        Highlighter hl = taLyrics.getHighlighter();
        hl.removeAllHighlights();
        for (BadRange range : badRanges.values()) {
            int from = range.from;
            int to = range.to + 1;
            try {
                hl.addHighlight(from, to, painter);
            } catch (BadLocationException ex) {
                // ignore
            }
        }
        this.badRanges = badRanges;
    }

    private SequencedCollection<String> getWarnings(int i) {
        Map.Entry<Integer, BadRange> beforeEntry = badRanges.floorEntry(i);
        Map.Entry<Integer, BadRange> afterEntry = badRanges.higherEntry(i);
        BadRange before = beforeEntry == null ? null : beforeEntry.getValue();
        BadRange after = afterEntry == null ? null : afterEntry.getValue();
        int leftDistance = before == null ? Integer.MAX_VALUE : i - before.to;
        int rightDistance = after == null ? Integer.MAX_VALUE : after.from - i;
        if (leftDistance < rightDistance) {
            if (leftDistance < 2) {
                return before.warnings;
            }
        } else if (leftDistance > rightDistance) {
            if (rightDistance < 2) {
                return after.warnings;
            }
        }
        return null;
    }

    private interface WarningConsumer {

        void accept(int i, String warning);
    }

    private static void checkSymbols(String text, WarningConsumer warn) {
        Character.UnicodeBlock currentWord = null;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (!Character.isLetterOrDigit(ch)) {
                if (ch >= 127) {
                    warn.accept(i, "Non-ASCII punctuation '" + ch + "'");
                }
                currentWord = null;
            } else {
                if (currentWord == null) {
                    currentWord = block;
                } else if (!currentWord.equals(block)) {
                    warn.accept(i, "Suspicious letter '" + ch + "' from " + block + " in a word of " + currentWord);
                }
            }
        }
    }
}
