package karaed.engine.steps.subs;

import ass.model.DialogLine;
import karaed.engine.ass.AssUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class MakeSubs {

    // todo: explore the effect of PlayResX/PlayResY
    private static final String HEADER = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 384
        PlayResY: 288
        ScaledBorderAndShadow: yes

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H000000FF,&H00FFFFFF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,5,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    // todo: optionally word-by-word
    public static void makeSubs(Path textFile, Path alignedFile, Path subsFile) throws IOException {
        List<List<CSegment>> lines = SyncChars.sync(textFile, alignedFile);

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(subsFile))) {
            HEADER.lines().forEach(pw::println);
            lines
                .stream()
                .map(MakeSubs::assLine)
                .filter(Objects::nonNull)
                .forEach(pw::println);
        }
    }

    private static void append(StringBuilder buf, double start, double end) {
        AssUtil.appendK(buf, end - start);
    }

    private static String assLine(List<CSegment> line) {
        double minStart = Double.NaN;
        double maxEnd = Double.NaN;
        for (CSegment ch : line) {
            Timestamps ts = ch.timestamps;
            if (ts == null)
                continue;
            if (Double.isNaN(minStart) || ts.start() < minStart) {
                minStart = ts.start();
            }
            if (Double.isNaN(maxEnd) || ts.end() > maxEnd) {
                maxEnd = ts.end();
            }
        }
        if (Double.isNaN(minStart) || Double.isNaN(maxEnd))
            return null;
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < line.size()) {
            CSegment ch = line.get(i);
            Timestamps ts = ch.timestamps;
            if (ts == null) {
                if (i > 0) {
                    double prevEnd = line.get(i - 1).timestamps.end();
                    StringBuilder spaces = new StringBuilder();
                    double end = maxEnd;
                    while (i < line.size()) {
                        CSegment chi = line.get(i);
                        if (chi.timestamps != null) {
                            end = chi.timestamps.start();
                            break;
                        }
                        spaces.append(chi.ch);
                        i++;
                    }
                    append(buf, prevEnd, end);
                    buf.append(spaces);
                } else {
                    buf.append(ch.ch);
                    i++;
                }
            } else {
                append(buf, ts.start(), ts.end());
                buf.append(ch.ch);
                i++;
            }
        }
        return assLine(minStart, maxEnd, buf.toString());
    }

    private static String assLine(double start, double end, String text) {
        return String.format(
            "Dialogue: 0,%s,%s,Default,,0,0,0,,%s",
            DialogLine.formatTimestamp(start), DialogLine.formatTimestamp(end), text
        );
    }
}
