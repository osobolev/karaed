package karaed.engine.steps.subs;

import ass.model.DialogLine;
import karaed.engine.ass.AssUtil;
import karaed.engine.opts.OAlign;
import karaed.engine.sync.SyncLyrics;
import karaed.engine.sync.TargetSegment;
import karaed.engine.sync.Timestamps;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

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
        SyncLyrics synced = SyncLyrics.create(textFile, alignedFile, options.words());
        List<List<TargetSegment>> lines = synced.getLines();
        double lastEnd = synced.lastEnd;

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(subsFile))) {
            long dummyFrames = (long) Math.ceil((lastEnd + 5.0) * VIDEO_FRAME_RATE);
            String header = String.format(HEADER, VIDEO_FRAME_RATE, dummyFrames);
            header.lines().forEach(pw::println);
            lines
                .stream()
                .map(line -> assLine(line, options))
                .filter(Objects::nonNull)
                .forEach(pw::println);
        }
    }

    private static void append(StringBuilder buf, String tag, double start, double end) {
        AssUtil.appendK(buf, tag, end - start);
    }

    private static String assLine(List<TargetSegment> line, OAlign options) {
        double minStart = Double.NaN;
        double maxEnd = Double.NaN;
        for (TargetSegment ch : line) {
            Timestamps ts = ch.timestamps();
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
        // 1. Skip all leading spaces (really should not happen)
        while (i < line.size()) {
            TargetSegment ch = line.get(i);
            if (ch.timestamps() != null)
                break;
            buf.append(ch.text);
            i++;
        }
        String tag = options.words() ? "K" : "k";
        while (i < line.size()) {
            // 2. Skip all words until next space
            int word1 = i;
            while (i < line.size()) {
                TargetSegment ch = line.get(i);
                if (ch.timestamps() == null)
                    break;
                i++;
            }
            // 3. Skip all spaces until next word
            int space1 = i;
            while (i < line.size()) {
                TargetSegment ch = line.get(i);
                if (ch.timestamps() != null)
                    break;
                i++;
            }

            for (int j = word1; j < space1 - 1; j++) {
                TargetSegment ch = line.get(j);
                Timestamps ts = ch.timestamps();
                append(buf, tag, ts.start(), ts.end());
                buf.append(ch.text);
            }
            TargetSegment lastWord = line.get(space1 - 1);
            double lastWordEnd;
            StringBuilder spaces = new StringBuilder();
            if (i > space1 && i < line.size()) {
                double prevWord = lastWord.timestamps().end();
                double nextWord = line.get(i).timestamps().start();
                if (!options.words()) {
                    lastWordEnd = lastWord.timestamps().end();
                    append(spaces, tag, prevWord, nextWord);
                } else {
                    lastWordEnd = nextWord;
                }
            } else {
                lastWordEnd = lastWord.timestamps().end();
            }
            for (int j = space1; j < i; j++) {
                TargetSegment ch = line.get(j);
                spaces.append(ch.text);
            }
            {
                append(buf, tag, lastWord.timestamps().start(), lastWordEnd);
                buf.append(lastWord.text);
            }
            buf.append(spaces);
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
