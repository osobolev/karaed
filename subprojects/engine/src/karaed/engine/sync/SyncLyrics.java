package karaed.engine.sync;

import karaed.engine.KaraException;
import karaed.engine.formats.aligned.Aligned;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class SyncLyrics {

    public final double lastEnd;
    private final List<TargetSegment> lyrics;

    private SyncLyrics(double lastEnd, List<TargetSegment> lyrics) {
        this.lastEnd = lastEnd;
        this.lyrics = lyrics;
    }

    private static List<TargetSegment> targetWordSegments(List<String> lines,
                                                          Function<String, List<Word>> splitToWords) {
        List<TargetSegment> lyrics = new ArrayList<>();
        for (String line : lines) {
            List<Word> words = splitToWords.apply(line);
            for (Word word : words) {
                lyrics.add(new TargetSegment(word.text(), word.letters()));
            }
            lyrics.add(new TargetSegment("\n", false));
        }
        return lyrics;
    }

    public static SyncLyrics create(Path textFile, Path alignedFile, boolean byWords) throws IOException {
        List<TargetSegment> lyrics;
        {
            Function<String, List<Word>> splitToWords;
            if (byWords) {
                splitToWords = SyncWords::splitToWords;
            } else {
                splitToWords = SyncChars::splitToWords;
            }

            List<String> textLines = Files.readAllLines(textFile);
            lyrics = targetWordSegments(textLines, splitToWords);
        }

        List<SrcSegment> aligned;
        double lastEnd = 0;
        {
            Function<Aligned, List<SrcSegment>> getSrcSegments;
            if (byWords) {
                getSrcSegments = SyncWords::srcWordSegments;
            } else {
                getSrcSegments = SyncChars::srcCharSegments;
            }

            Aligned alignedLyrics = JsonUtil.readFile(alignedFile, Aligned.class);
            aligned = getSrcSegments.apply(alignedLyrics);
            if (!aligned.isEmpty()) {
                lastEnd = aligned.getLast().timestamps().end();
            }
        }

        int il = 0;
        int ia = 0;
        while (true) {
            while (il < lyrics.size()) {
                TargetSegment cl = lyrics.get(il);
                if (cl.letters)
                    break;
                il++;
            }
            if (il >= lyrics.size())
                break;
            TargetSegment cl = lyrics.get(il);
            SrcSegment ca = ia < aligned.size() ? aligned.get(ia) : null;
            if (ca == null || !ca.text().equalsIgnoreCase(cl.text)) {
                throw new KaraException(String.format(
                    "Unexpected misalignment between %s and %s",
                    textFile.getFileName(), alignedFile.getFileName()
                ));
            }

            cl.timestamps = ca.timestamps();
            il++;
            ia++;
        }

        return new SyncLyrics(lastEnd, lyrics);
    }

    public List<List<TargetSegment>> getLines() {
        List<List<TargetSegment>> lines = new ArrayList<>();
        lines.add(new ArrayList<>());
        for (TargetSegment cs : lyrics) {
            String ch = cs.text;
            if ("\n".equals(ch)) {
                lines.add(new ArrayList<>());
                continue;
            }
            List<TargetSegment> currentLine = lines.getLast();
            currentLine.add(cs);
        }
        return lines;
    }
}
