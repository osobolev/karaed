package karaed.engine.steps.youtube;

import karaed.engine.opts.OInput;
import karaed.tools.ProcRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Youtube {

    public static void download(ProcRunner runner, OInput input, Path audio) throws IOException, InterruptedException {
        if (input.url() != null) {
            runner.runPythonExe(
                "yt-dlp",
                "--write-info-json", "-k",
                "--extract-audio",
                "--audio-format", "mp3",
                "--output", audio.toString(),
                input.url()
            );
            // todo: possibly cut audio/video
        } else if (input.file() != null) {
            Files.copy(Path.of(input.file()), audio);
            // todo: possibly cut audio
        } else {
            // todo: error
            return;
        }
    }
}
