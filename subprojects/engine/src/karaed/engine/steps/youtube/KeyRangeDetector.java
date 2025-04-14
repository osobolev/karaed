package karaed.engine.steps.youtube;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import karaed.engine.formats.ffprobe.FFFormat;
import karaed.engine.formats.ffprobe.FFFrame;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

final class KeyRangeDetector {

    private final ProcRunner runner;
    private final CutRange original;
    private final Double start;
    private final Double end;

    KeyRangeDetector(ProcRunner runner, CutRange range) {
        this.runner = runner;
        this.original = range;
        this.start = range.start;
        this.end = range.end;
    }

    private final class FrameAcc {

        private Double lastBeforeStart = null;
        private Double firstAfterEnd = null;

        void add(double ts) {
            if (start != null) {
                if (ts <= start.doubleValue() && (lastBeforeStart == null || ts > lastBeforeStart.doubleValue())) {
                    lastBeforeStart = ts;
                }
            }
            if (end != null) {
                if (firstAfterEnd == null && ts >= end.doubleValue()) {
                    firstAfterEnd = ts;
                }
            }
        }
    }

    private static Double getFrameTime(Double time, Function<FrameAcc, Double> getAccTime, FrameAcc keyAcc, FrameAcc nonKeyAcc) {
        if (time == null)
            return null;
        FrameAcc[] accs = {keyAcc, nonKeyAcc};
        for (FrameAcc acc : accs) {
            Double accTime = getAccTime.apply(acc);
            if (accTime != null)
                return accTime;
        }
        return time;
    }

    private CutRange parseFrameStream(double duration, InputStream in) throws IOException {
        JsonReader tok = JsonUtil.GSON.newJsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        tok.beginObject();
        tok.nextName();
        tok.beginArray();
        FrameAcc keyAcc = new FrameAcc();
        FrameAcc nonKeyAcc = new FrameAcc();
        long prevPercent = -1;
        while (tok.peek() != JsonToken.END_ARRAY) {
            FFFrame frame = JsonUtil.GSON.fromJson(tok, FFFrame.class);
            try {
                double ts = Double.parseDouble(frame.best_effort_timestamp_time());
                long percent = Math.round(ts / duration * 100.0);
                if (percent != prevPercent) {
                    runner.log(false, String.format("Scanning frames: %s%%\r", percent));
                    prevPercent = percent;
                }
                String type = frame.pict_type();
                if ("I".equalsIgnoreCase(type)) {
                    keyAcc.add(ts);
                } else {
                    nonKeyAcc.add(ts);
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        in.transferTo(OutputStream.nullOutputStream());
        runner.log(false, System.lineSeparator());
        Double realStart = getFrameTime(start, acc -> acc.lastBeforeStart, keyAcc, nonKeyAcc);
        Double realEnd = getFrameTime(end, acc -> acc.firstAfterEnd, keyAcc, nonKeyAcc);
        return new CutRange(realStart, realEnd);
    }

    CutRange getRealCut(Path file) throws IOException, InterruptedException {
        double duration;
        {
            FFFormat format = FileFormatUtil.getFormat(runner, file);
            duration = Double.parseDouble(format.duration());
        }
        {
            AtomicReference<CutRange> realCut = new AtomicReference<>(original);
            try (PipedInputStream sink = new PipedInputStream()) {
                Thread sinkReader = new Thread(() -> {
                    try {
                        CutRange range = parseFrameStream(duration, sink);
                        realCut.set(range);
                    } catch (IOException ex) {
                        // ignore
                    }
                });

                runner.runFFProbeStreaming(
                    List.of(
                        "-print_format", "json",
                        "-select_streams", "v",
                        "-skip_frame", "nokey",
                        "-show_frames",
                        "-show_entries", "frame=best_effort_timestamp_time,pict_type",
                        file.toString()
                    ),
                    stdout -> {
                        try (PipedOutputStream out = new PipedOutputStream(sink)) {
                            sinkReader.start();
                            stdout.transferTo(out);
                        } catch (IOException ex) {
                            // ignore
                        }
                    }
                );

                sinkReader.join();
            }
            return realCut.get();
        }
    }
}
