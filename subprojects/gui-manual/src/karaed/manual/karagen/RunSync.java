package karaed.manual.karagen;

import karaed.manual.FileAudioSource;
import karaed.manual.gui.save.SaveData;
import karaed.io.ProcUtil;
import karaed.json.JsonUtil;
import karaed.manual.karagen.json.aligned.AlignSegment;
import karaed.manual.karagen.json.aligned.Aligned;
import karaed.manual.karagen.json.aligned.WordSegment;
import karaed.manual.karagen.json.transcription.TransSegment;
import karaed.manual.karagen.json.transcription.Transcription;
import karaed.manual.model.AudioSource;
import karaed.manual.model.Range;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RunSync {

    public static void sync(AudioSource source, Range range, Path voice, Path fast, Path aligned) throws IOException, UnsupportedAudioFileException, InterruptedException {
        try (OutputStream out = Files.newOutputStream(voice)) {
            source.cut(range.from(), range.to(), out);
        }
        ProcUtil.runCommand(
            "align",
            Path.of("C:\\Users\\sobol\\.jkara\\python\\python.exe"),
            List.of(
                "align.py", // todo
                voice.toAbsolutePath().toString(),
                fast.toAbsolutePath().toString(),
                aligned.toAbsolutePath().toString()
            ),
            List.of(Path.of("C:\\Users\\sobol\\.jkara\\ffmpeg\\bin")),
            null, null
        );
    }

    public static void main(String[] args) throws Exception {
        ProcUtil.registerShutdown();

        SaveData data = JsonUtil.readFile(Path.of("test.kara"), SaveData.class);
        AudioSource source = new FileAudioSource(new File(data.vocalsPath()));
        List<Range> ranges = data.ranges();
        List<Aligned> alignedRanges = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
//            Range range = ranges.get(i);
//            System.out.println(range);
//            String line = data.editedLines().get(i);
//            Transcription transcription = new Transcription("es", Collections.singletonList(new TransSegment(
//                0.0, (range.to() - range.from()) / 44_100.0, line
//            )));
//            Path fast = Path.of("fast" + i + ".json");
//            JsonUtil.writeFile(fast, transcription);
            Path aligned = Path.of("aligned" + i + ".json");
//            Path tmp = Path.of("piece" + i + ".wav"); // todo: use real temp file!!!
//            sync(source, range, tmp, fast, aligned);
            Aligned alignedData = JsonUtil.readFile(aligned, Aligned.class);
            alignedRanges.add(alignedData);
        }
        List<TransSegment> transSegments = new ArrayList<>();
        List<AlignSegment> segments = new ArrayList<>();
        List<WordSegment> wordSegments = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            String line = data.editedLines().get(i);
            Aligned aligned = alignedRanges.get(i);
            Aligned shifted = aligned.shift(range.from() / 44_100.0);
            transSegments.add(new TransSegment(range.from() / 44_100.0, range.to() / 44_100.0, line));
            segments.addAll(shifted.segments());
            wordSegments.addAll(shifted.wordSegments());
        }
        Transcription transcription = new Transcription("es", transSegments);
        JsonUtil.writeFile(Path.of("fast.json"), transcription);

        Aligned allAligned = new Aligned(segments, wordSegments);
        JsonUtil.writeFile(Path.of("aligned.json"), allAligned);
    }
}
