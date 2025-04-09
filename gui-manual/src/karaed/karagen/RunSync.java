package karaed.karagen;

import karaed.model.AudioSource;
import karaed.model.Range;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class RunSync {

    public static void sync(AudioSource source, Range range) throws IOException, UnsupportedAudioFileException, InterruptedException {
        Path tmp = Paths.get("tmp.wav"); // todo: use real temp file!!!
        try (OutputStream out = Files.newOutputStream(tmp)) {
            source.cut(range.from(), range.to(), out);
        }
        ProcessBuilder pb = new ProcessBuilder(
            "C:\\Users\\sobol\\.jkara\\python\\python.exe", // todo
            "transcribe.py", // todo
            tmp.toAbsolutePath().toString(),
            "test.json",
            "es" // todo: detect
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int exitCode = p.waitFor();
        System.out.println(exitCode);
    }
}
