package karaed.engine.steps.youtube;

import karaed.engine.formats.ranges.Range;
import karaed.engine.opts.OCut;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CutRange {

    final Double start;
    final Double end;

    CutRange(Double start, Double end) {
        this.start = start;
        this.end = end;
    }

    static CutRange create(OCut cut) {
        String start = cut.from();
        String end = cut.to();
        Double secStart;
        if (start != null) {
            secStart = OCut.parseTime(start);
        } else {
            secStart = null;
        }
        Double secEnd;
        if (end != null) {
            secEnd = OCut.parseTime(end);
        } else {
            secEnd = null;
        }
        if (secStart == null && secEnd == null)
            return null;
        return new CutRange(secStart, secEnd);
    }

    void cutFile(ToolRunner runner, Path file, Path outFile) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of(
            "-y",
            "-i", file.toString()
        ));
        if (start != null) {
            args.addAll(List.of(
                "-ss", start.toString()
            ));
        }
        if (end != null) {
            args.addAll(List.of(
                "-to", end.toString()
            ));
        }
        args.addAll(List.of(
            "-c:v", "copy",
            "-c:a", "copy",
            "-avoid_negative_ts", "make_zero",
            outFile.toString()
        ));
        runner.runFFMPEG(args);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (start != null) {
            buf.append(Range.formatTime(start.floatValue()));
        } else {
            buf.append(Range.formatTime(0f));
        }
        buf.append('-');
        if (end != null) {
            buf.append(Range.formatTime(end.floatValue()));
        } else {
            buf.append("inf");
        }
        return buf.toString();
    }
}
