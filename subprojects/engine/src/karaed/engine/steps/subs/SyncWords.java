package karaed.engine.steps.subs;

import karaed.engine.KaraException;
import karaed.engine.ass.AssUtil;
import karaed.engine.formats.aligned.AlignSegment;
import karaed.engine.formats.aligned.Aligned;
import karaed.engine.formats.aligned.WordSegment;

import java.util.ArrayList;
import java.util.List;

final class SyncWords extends SyncAny {

    private record Word(
        String text,
        boolean letters
    ) {}

    private static final class WordTokenizer {

        final List<Word> words = new ArrayList<>();
        final StringBuilder buf = new StringBuilder();
        boolean inWord = false;

        private void add() {
            if (!buf.isEmpty()) {
                words.add(new Word(buf.toString(), inWord));
                buf.setLength(0);
            }
        }

        List<Word> splitToWords(String text) {
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (AssUtil.isLetter(ch)) {
                    if (!inWord) {
                        add();
                        inWord = true;
                    }
                } else {
                    if (inWord) {
                        add();
                        inWord = false;
                    }
                }
                buf.append(ch);
            }
            add();
            return words;
        }
    }

    private static List<Word> splitToWords(String text) {
        return new WordTokenizer().splitToWords(text);
    }

    static List<TargetSegment> targetWordSegments(List<String> lines) {
        List<TargetSegment> lyrics = new ArrayList<>();
        for (String line : lines) {
            List<Word> words = splitToWords(line);
            for (Word word : words) {
                lyrics.add(new TargetSegment(word.text, word.letters));
            }
            lyrics.add(new TargetSegment("\n", false));
        }
        return lyrics;
    }

    static List<SrcSegment> srcWordSegments(Aligned alignedLyrics) {
        List<SrcSegment> aligned = new ArrayList<>();
        for (int i = 0; i < alignedLyrics.segments().size(); i++) {
            AlignSegment segment = alignedLyrics.segments().get(i);
            String segText = segment.text();
            for (int j = 0; j < segment.words().size(); j++) {
                WordSegment ws = segment.words().get(j);
                List<Word> words = splitToWords(ws.word());
                String word1 = null;
                for (Word word : words) {
                    if (word.letters()) {
                        if (word1 == null) {
                            word1 = word.text();
                        } else {
                            String where = getWhere(i, segText);
                            throw new KaraException(String.format(
                                "Unexpected multiple words in \"%s\", word \"%s\"", where, ws.word()
                            ));
                        }
                    }
                }
                if (word1 == null)
                    continue;
                Timestamps timestamps = checkTimestamps(ws.start(), ws.end(), i, segText, "word", j);
                aligned.add(new SrcSegment(word1, timestamps));
            }
        }
        return aligned;
    }
}
