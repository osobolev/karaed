package karaed.engine.steps.align;

import karaed.engine.KaraException;
import karaed.engine.audio.AudioSource;
import karaed.engine.audio.FileAudioSource;
import karaed.engine.formats.aligned.AlignSegment;
import karaed.engine.formats.aligned.Aligned;
import karaed.engine.formats.aligned.WordSegment;
import karaed.engine.formats.ranges.Range;
import karaed.engine.formats.ranges.Ranges;
import karaed.engine.formats.transcription.TransSegment;
import karaed.engine.formats.transcription.Transcription;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Align {

    private static Aligned alignRange(ProcRunner runner, Path tmpDir, AudioSource source, double frameRate, int i, Range range, String line) throws IOException, UnsupportedAudioFileException, InterruptedException {
        Path voice = tmpDir.resolve("piece" + i + ".wav");
        try (OutputStream out = Files.newOutputStream(voice)) {
            source.cut(range.from(), range.to(), out);
        }

        String language = "es"; // todo!!! detect it

        TransSegment segment = new TransSegment(0.0, (range.to() - range.from()) / frameRate, line);
        Transcription transcription = new Transcription(language, Collections.singletonList(segment));

        Path fast = tmpDir.resolve("fast" + i + ".json");
        JsonUtil.writeFile(fast, transcription);

        Path aligned = tmpDir.resolve("aligned" + i + ".json");
        // todo: possibly run transcription first for better voice range detection???
        runner.runPythonScript(
            "scripts/align.py",
            voice.toAbsolutePath().toString(),
            fast.toAbsolutePath().toString(),
            aligned.toAbsolutePath().toString()
        );
        return JsonUtil.readFile(aligned, Aligned.class);
    }

    public static void align(ProcRunner runner, Path vocals, Path rangesFile, Path tmpDir, Path alignedFile) throws IOException, UnsupportedAudioFileException, InterruptedException {
        Ranges data = JsonUtil.readFile(rangesFile, Ranges.class);
        List<Range> ranges = data.ranges();
        List<String> lines = data.lines();
        if (ranges.size() != lines.size())
            throw new KaraException("Vocal ranges and lyrics lines must have one-to-one correspondence");
        AudioSource source = new FileAudioSource(vocals.toFile());
        Files.createDirectories(tmpDir);
        List<Aligned> alignedRanges = new ArrayList<>();
        // todo: use audio format to detect frame rate:
        double frameRate = 44_100.0;
        for (int i = 0; i < ranges.size(); i++) {
            runner.log(false, String.format("Aligning range %d of %d", i + 1, ranges.size()));
            Range range = ranges.get(i);
            String line = lines.get(i);
            Aligned alignedData = alignRange(runner, tmpDir, source, frameRate, i, range, line);
            alignedRanges.add(alignedData);
        }
//        List<TransSegment> transSegments = new ArrayList<>();
        List<AlignSegment> segments = new ArrayList<>();
        List<WordSegment> wordSegments = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
//            String line = lines.get(i);
            Aligned aligned = alignedRanges.get(i);
            Aligned shifted = aligned.shift(range.from() / frameRate);
//            transSegments.add(new TransSegment(range.from() / frameRate, range.to() / frameRate, line));
            segments.addAll(shifted.segments());
            wordSegments.addAll(shifted.wordSegments());
        }
//        String language = "es"; // todo: detect
//        Transcription transcription = new Transcription(language, transSegments);
//        JsonUtil.writeFile(Path.of("fast.json"), transcription); // todo: do we really need it now???

        Aligned allAligned = new Aligned(segments, wordSegments);
        JsonUtil.writeFile(alignedFile, allAligned);
    }
}
