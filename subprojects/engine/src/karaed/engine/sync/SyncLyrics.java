package karaed.engine.sync;

import karaed.engine.KaraException;
import karaed.engine.formats.aligned.Aligned;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SyncLyrics {

    public final double lastEnd;
    private final List<TargetSegment> lyrics;
    public final List<Timestamps> backvocalRanges;

    private SyncLyrics(double lastEnd, List<TargetSegment> lyrics, List<Timestamps> backvocalRanges) {
        this.lastEnd = lastEnd;
        this.lyrics = lyrics;
        this.backvocalRanges = backvocalRanges;
    }

    private record TargetRange(
        TargetSegment start,
        TargetSegment end
    ) {}

    private static final class ToTarget {

        final List<TargetSegment> lyrics = new ArrayList<>();
        final List<TargetRange> backvocalRanges = new ArrayList<>();
        boolean inBackvocal = false;
        TargetSegment backStart = null;
        TargetSegment backEnd = null;

        private void addRange() {
            if (backStart != null && backEnd != null) {
                backvocalRanges.add(new TargetRange(backStart, backEnd));
            }
        }

        private void add(Word word) {
            TargetSegment target = new TargetSegment(word.text(), word.letters());
            lyrics.add(target);
            if (word.inBackvocal()) {
                inBackvocal = true;
                if (word.letters()) {
                    if (backStart == null) {
                        backStart = target;
                    }
                    backEnd = target;
                }
            } else {
                if (inBackvocal) {
                    addRange();
                    backStart = null;
                    backEnd = null;
                    inBackvocal = false;
                }
            }
        }

        private void finish() {
            if (inBackvocal) {
                addRange();
            }
        }

        void convert(List<String> lines,
                     BiFunction<String, BackvocalState, List<Word>> splitToWords) {
            BackvocalState backvocal = BackvocalState.create();
            for (String line : lines) {
                List<Word> words = splitToWords.apply(line, backvocal);
                for (Word word : words) {
                    add(word);
                }
                lyrics.add(new TargetSegment("\n", false));
            }
            finish();
        }
    }

    public static SyncLyrics create(Path textFile, Path alignedFile, boolean byWords) throws IOException {
        List<TargetSegment> lyrics;
        List<TargetRange> backvocalRanges;
        {
            BiFunction<String, BackvocalState, List<Word>> splitToWords;
            if (byWords) {
                splitToWords = SyncWords::splitToWords;
            } else {
                splitToWords = SyncChars::splitToWords;
            }

            List<String> textLines = Files.readAllLines(textFile);
            ToTarget toTarget = new ToTarget();
            toTarget.convert(textLines, splitToWords);
            lyrics = toTarget.lyrics;
            backvocalRanges = toTarget.backvocalRanges;
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

        List<Timestamps> backvocals = new ArrayList<>();
        for (TargetRange range : backvocalRanges) {
            if (range.start.timestamps != null && range.end.timestamps != null) {
                double start = range.start.timestamps.start();
                double end = range.end.timestamps.end();
                backvocals.add(new Timestamps(start, end));
            }
        }

        return new SyncLyrics(lastEnd, lyrics, backvocals);
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
