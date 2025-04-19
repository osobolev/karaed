package karaed.engine.steps.subs;

import karaed.engine.ass.AssUtil;
import karaed.engine.formats.aligned.AlignSegment;
import karaed.engine.formats.aligned.Aligned;
import karaed.engine.formats.aligned.CharSegment;

import java.util.ArrayList;
import java.util.List;

final class SyncChars extends SyncAny {

    static List<TargetSegment> targetCharSegments(List<String> lines) {
        String text = String.join("\n", lines);
        List<TargetSegment> lyrics = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            lyrics.add(new TargetSegment(String.valueOf(ch), AssUtil.isLetter(ch)));
        }
        return lyrics;
    }

    static List<SrcSegment> srcCharSegments(Aligned alignedLyrics) {
        List<SrcSegment> aligned = new ArrayList<>();
        for (int i = 0; i < alignedLyrics.segments().size(); i++) {
            AlignSegment segment = alignedLyrics.segments().get(i);
            String segText = segment.text();
            for (int j = 0; j < segment.chars().size(); j++) {
                CharSegment cs = segment.chars().get(j);
                char ch = cs.getChar();
                if (!AssUtil.isLetter(ch))
                    continue;
                Timestamps timestamps = checkTimestamps(cs.start(), cs.end(), i, segText, "char", j);
                aligned.add(new SrcSegment(String.valueOf(ch), timestamps));
            }
        }
        return aligned;
    }
}
