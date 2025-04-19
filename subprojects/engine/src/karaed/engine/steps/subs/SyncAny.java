package karaed.engine.steps.subs;

import karaed.engine.KaraException;
import karaed.engine.formats.aligned.Aligned;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class SyncAny {

    static String getWhere(int iseg, String segment) {
        return segment == null ? "segment " + iseg : "'" + segment + "'";
    }

    private static double checkTimestamp(Double t, String key, int iseg, String segment, String entity, int index) {
        if (t == null || t.isNaN()) {
            String where = getWhere(iseg, segment);
            throw new KaraException(String.format(
                "Missing \"%s\" timestamp at %s, %s %s", key, where, entity, index
            ));
        }
        return t.doubleValue();
    }

    static Timestamps checkTimestamps(Double start, Double end, int iseg, String segment, String entity, int index) {
        double from = checkTimestamp(start, "start", iseg, segment, entity, index);
        double to = checkTimestamp(end, "end", iseg, segment, entity, index);
        return new Timestamps(from, to);
    }

    static double sync(Path textFile, Path alignedFile,
                       Function<Aligned, List<SrcSegment>> getSrcSegments,
                       Function<List<String>, List<TargetSegment>> getTargetSegments,
                       List<List<TargetSegment>> lines) throws IOException {
        List<TargetSegment> lyrics;
        {
            List<String> textLines = Files.readAllLines(textFile);
            lyrics = getTargetSegments.apply(textLines);
        }

        List<SrcSegment> aligned;
        double lastEnd = 0;
        {
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
            // todo: both must end at the same time!!!
            if (il >= lyrics.size())
                break;
            if (ia >= aligned.size())
                break;
            TargetSegment cl = lyrics.get(il);
            SrcSegment ca = aligned.get(ia);
            if (!ca.text().equalsIgnoreCase(cl.text)) {
                throw new KaraException(String.format(
                    "Unexpected misalignment between %s and %s",
                    textFile.getFileName(), alignedFile.getFileName()
                ));
            }

            cl.timestamps = ca.timestamps();
            il++;
            ia++;
        }

        splitToLines(lyrics, lines);
        return lastEnd;
    }

    private static void splitToLines(List<TargetSegment> lyrics, List<List<TargetSegment>> lines) {
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
    }
}
