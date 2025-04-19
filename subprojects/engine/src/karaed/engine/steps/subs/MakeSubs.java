package karaed.engine.steps.subs;

import ass.model.DialogLine;
import karaed.engine.ass.AssUtil;
import karaed.engine.formats.aligned.Aligned;
import karaed.engine.opts.OAlign;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class MakeSubs {

    private static final double VIDEO_FRAME_RATE = 23.976;

    // todo: explore the effect of PlayResX/PlayResY
    private static final String HEADER = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 384
        PlayResY: 288
        ScaledBorderAndShadow: yes

        [Aegisub Project Garbage]
        Audio File: audio.mp3
        Video File: ?dummy:%s:%s:640:480:47:163:254:
        Video AR Value: 1.333333

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H000000FF,&H00FFFFFF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,5,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    public static void makeSubs(Path textFile, Path alignedFile, OAlign options, Path subsFile) throws IOException {
        Function<Aligned, List<SrcSegment>> getSrcSegments;
        Function<List<String>, List<TargetSegment>> getTargetSegments;
        if (options.words()) {
            getSrcSegments = SyncWords::srcWordSegments;
            getTargetSegments = SyncWords::targetWordSegments;
        } else {
            getSrcSegments = SyncChars::srcCharSegments;
            getTargetSegments = SyncChars::targetCharSegments;
        }
        List<List<TargetSegment>> lines = new ArrayList<>();
        double lastEnd = SyncAny.sync(textFile, alignedFile, getSrcSegments, getTargetSegments, lines);

        String tag = options.words() ? "K" : "k";
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(subsFile))) {
            long dummyFrames = (long) Math.ceil((lastEnd + 5.0) * VIDEO_FRAME_RATE);
            String header = String.format(HEADER, VIDEO_FRAME_RATE, dummyFrames);
            header.lines().forEach(pw::println);
            lines
                .stream()
                .map(line -> assLine(line, tag))
                .filter(Objects::nonNull)
                .forEach(pw::println);
        }
    }

    private static void append(StringBuilder buf, String tag, double start, double end) {
        AssUtil.appendK(buf, tag, end - start);
    }

    private static String assLine(List<TargetSegment> line, String tag) {
        double minStart = Double.NaN;
        double maxEnd = Double.NaN;
        for (TargetSegment ch : line) {
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
            TargetSegment ch = line.get(i);
            Timestamps ts = ch.timestamps;
            if (ts == null) {
                if (i > 0) {
                    double prevEnd = line.get(i - 1).timestamps.end();
                    StringBuilder spaces = new StringBuilder();
                    double end = maxEnd;
                    while (i < line.size()) {
                        TargetSegment chi = line.get(i);
                        if (chi.timestamps != null) {
                            end = chi.timestamps.start();
                            break;
                        }
                        spaces.append(chi.text);
                        i++;
                    }
                    append(buf, tag, prevEnd, end);
                    buf.append(spaces);
                } else {
                    buf.append(ch.text);
                    i++;
                }
            } else {
                append(buf, tag, ts.start(), ts.end());
                buf.append(ch.text);
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
