package karaed.engine.steps.youtube;

import karaed.engine.formats.ranges.Range;
import karaed.engine.opts.OCut;
import karaed.engine.video.FileStreamUtil;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CutRange {

    private final Double prepend;
    private final Double start;
    private final Double end;

    CutRange(Double start, Double end) {
        if (start != null && start.doubleValue() < 0) {
            this.prepend = -start.doubleValue();
            this.start = null;
        } else {
            this.prepend = null;
            this.start = start;
        }
        this.end = end;
    }

    static CutRange create(OCut cut) {
        String start = cut.from();
        String end = cut.to();
        Double secStart;
        if (start != null) {
            secStart = OCut.parseTime(start, true);
        } else {
            secStart = null;
        }
        Double secEnd;
        if (end != null) {
            secEnd = OCut.parseTime(end, false);
        } else {
            secEnd = null;
        }
        if (secStart == null && secEnd == null)
            return null;
        return new CutRange(secStart, secEnd);
    }

    CutRange toRealCut(ToolRunner runner, Path fullVideo) throws IOException, InterruptedException {
        if (start == null && end == null) {
            // Special case: nothing is removed, only added
            return this;
        }
        return new KeyRangeDetector(runner, start, end).getRealCut(fullVideo);
    }

    void cutFile(ToolRunner runner, Path file, Path outFile) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of(
            "-y", "-stats",
            "-i", file.toString()
        ));
        if (prepend != null) {
            boolean audioOnly = FileStreamUtil.listVideoStreams(runner, file).isEmpty();
            int delay = Math.round(prepend.floatValue() * 1000);
            String vfilter;
            String afilter;
            if (end == null) {
                vfilter = String.format(
                    "[0:v]tpad=start_duration=%s:start_mode=clone[v]",
                    prepend
                );
                afilter = String.format(
                    "[0:a]adelay=delays=%s:all=1[a]",
                    delay
                );
            } else {
                double duration = prepend.doubleValue() + end.doubleValue();
                vfilter = String.format(
                    "[0:v]tpad=start_duration=%s:start_mode=clone,trim=duration=%s[v]",
                    prepend, duration
                );
                afilter = String.format(
                    "[0:a]adelay=delays=%s:all=1,atrim=duration=%s[a]",
                    delay, duration
                );
            }
            if (audioOnly) {
                args.addAll(List.of(
                    "-filter_complex", afilter,
                    "-map", "[a]",
                    "-q:a", "2"
                ));
            } else {
                args.addAll(List.of(
                    "-filter_complex", vfilter + "; " + afilter,
                    "-map", "[v]", "-map", "[a]",
                    "-crf", "18"
                ));
            }
        } else {
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
                "-avoid_negative_ts", "make_zero"
            ));
        }
        args.add(outFile.toString());
        runner.run().ffmpeg(args);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (prepend != null) {
            buf.append('-').append(Range.formatTime(prepend.floatValue()));
        } else if (start != null) {
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
