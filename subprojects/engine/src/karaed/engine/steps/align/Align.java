package karaed.engine.steps.align;

import karaed.engine.KaraException;
import karaed.engine.audio.AudioSource;
import karaed.engine.audio.FileAudioSource;
import karaed.engine.formats.aligned.AlignSegment;
import karaed.engine.formats.aligned.Aligned;
import karaed.engine.formats.aligned.WordSegment;
import karaed.engine.formats.lang.LangDetect;
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
import java.util.*;

public final class Align {

    private static Path voice(Path tmpDir, int i) {
        return tmpDir.resolve("piece" + i + ".wav");
    }

    private static Aligned alignRange(ProcRunner runner, Path tmpDir, float frameRate,
                                      String language,
                                      int i, Range range, String line) throws IOException, InterruptedException {
        Path voice = voice(tmpDir, i);

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

    private static String detectLanguage(ProcRunner runner, Path tmpDir, List<Range> ranges) throws IOException, InterruptedException {
        LinkedHashMap<String, Double> langs = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(ranges.size(), 3); i++) {
            Path voice = voice(tmpDir, i);
            LangDetect ld = runner.runPythonScript(
                "scripts/language.py", rdr -> JsonUtil.parse(rdr, LangDetect.class),
                voice.toAbsolutePath().toString()
            );
            if (ld.langprob() > 0.5)
                return ld.language();
            langs.merge(ld.language(), ld.langprob(), Double::sum);
        }
        return langs
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("en");
    }

    private record FilteredRanges(
        List<Range> ranges,
        List<String> lines
    ) {}

    private static FilteredRanges filterRanges(Ranges data) {
        List<Range> ranges = data.ranges();
        List<String> lines = data.lines().stream().filter(line -> !line.trim().isEmpty()).toList();
        if (ranges.size() != lines.size())
            throw new KaraException("Vocal ranges and lyrics lines must have one-to-one correspondence");
        List<Range> filteredRanges = new ArrayList<>();
        List<String> filteredLines = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            String line = lines.get(i);
            if ("#".equals(line.trim()))
                continue;
            filteredRanges.add(range);
            filteredLines.add(line.trim());
        }
        return new FilteredRanges(filteredRanges, filteredLines);
    }

    public static void align(ProcRunner runner, Path vocals, Path rangesFile, Path tmpDir, Path alignedFile) throws IOException, UnsupportedAudioFileException, InterruptedException {
        FilteredRanges data = filterRanges(JsonUtil.readFile(rangesFile, Ranges.class));
        List<Range> ranges = data.ranges();
        List<String> lines = data.lines();
        AudioSource source = new FileAudioSource(vocals.toFile());
        Files.createDirectories(tmpDir);
        float frameRate = source.getFormat().getFrameRate();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            Path voice = voice(tmpDir, i);
            try (OutputStream out = Files.newOutputStream(voice)) {
                source.cut(range.from(), range.to(), out);
            }
        }
        runner.println("Detecting language..."); // todo: make manual language choice option
        String language = detectLanguage(runner, tmpDir, ranges);
        runner.println("Detected language: " + language);
        List<Aligned> alignedRanges = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            runner.println(String.format("Aligning range %d of %d", i + 1, ranges.size()));
            Range range = ranges.get(i);
            String line = lines.get(i);
            Aligned alignedData = alignRange(runner, tmpDir, frameRate, language, i, range, line);
            alignedRanges.add(alignedData);
        }
        List<AlignSegment> segments = new ArrayList<>();
        List<WordSegment> wordSegments = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            Aligned aligned = alignedRanges.get(i);
            Aligned shifted = aligned.shift(range.from() / frameRate);
            segments.addAll(shifted.segments());
            wordSegments.addAll(shifted.wordSegments());
        }

        Aligned allAligned = new Aligned(segments, wordSegments);
        JsonUtil.writeFile(alignedFile, allAligned);
    }
}
