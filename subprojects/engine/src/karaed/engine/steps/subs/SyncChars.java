package karaed.engine.steps.subs;

import karaed.engine.KaraException;
import karaed.engine.ass.AssUtil;
import karaed.engine.formats.aligned.AlignSegment;
import karaed.engine.formats.aligned.Aligned;
import karaed.engine.formats.aligned.CharSegment;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SyncChars {

    private static void checkTimestamp(Double t, String key, int iseg, String segment, int ichar) {
        if (t == null || t.isNaN()) {
            String where = segment == null ? "segment " + iseg : "'" + segment + "'";
            throw new KaraException(String.format("Missing \"%s\" timestamp at %s, char %s", key, where, ichar));
        }
    }

    static double sync(Path textFile, Path alignedFile, List<List<CSegment>> lines) throws IOException {
        List<CSegment> lyrics = new ArrayList<>();
        {
            String text = String.join("\n", Files.readAllLines(textFile));
            for (int i = 0; i < text.length(); i++) {
                lyrics.add(new CSegment(text.charAt(i)));
            }
        }

        List<CharSegment> aligned = new ArrayList<>();
        double lastEnd = 0;
        {
            Aligned alignedLyrics = JsonUtil.readFile(alignedFile, Aligned.class);
            for (int i = 0; i < alignedLyrics.segments().size(); i++) {
                AlignSegment segment = alignedLyrics.segments().get(i);
                String segText = segment.text();
                for (int j = 0; j < segment.chars().size(); j++) {
                    CharSegment cs = segment.chars().get(j);
                    checkTimestamp(cs.start(), "start", i, segText, j);
                    checkTimestamp(cs.end(), "end", i, segText, j);
                    aligned.add(cs);
                    lastEnd = Math.max(lastEnd, cs.end().doubleValue());
                }
            }
        }

        int il = 0;
        int ia = 0;
        while (true) {
            while (il < lyrics.size()) {
                char cr = lyrics.get(il).ch;
                if (AssUtil.isLetter(cr))
                    break;
                il++;
            }
            if (il >= lyrics.size())
                break;
            while (ia < aligned.size()) {
                char ca = aligned.get(ia).getChar();
                if (AssUtil.isLetter(ca))
                    break;
                ia++;
            }
            if (ia >= aligned.size())
                break;
            CSegment cl = lyrics.get(il);
            CharSegment ca = aligned.get(ia);
            if (!String.valueOf(ca.getChar()).equalsIgnoreCase(String.valueOf(cl.ch))) {
                throw new IllegalStateException(String.format(
                    "Unexpected misalignment between %s and %s",
                    textFile.getFileName(), alignedFile.getFileName()
                ));
            }

            cl.timestamps = new Timestamps(ca.start().doubleValue(), ca.end().doubleValue());
            il++;
            ia++;
        }

        splitToLines(lyrics, lines);
        return lastEnd;
    }

    private static void splitToLines(List<CSegment> lyrics, List<List<CSegment>> lines) {
        lines.add(new ArrayList<>());
        for (CSegment cs : lyrics) {
            char ch = cs.ch;
            if (ch == '\n') {
                lines.add(new ArrayList<>());
                continue;
            }
            List<CSegment> currentLine = lines.get(lines.size() - 1);
            currentLine.add(cs);
        }
    }
}
