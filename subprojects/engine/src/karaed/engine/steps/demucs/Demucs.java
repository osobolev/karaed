package karaed.engine.steps.demucs;

import karaed.engine.opts.ODemucs;
import karaed.tools.ProcRunner;

import java.io.IOException;
import java.nio.file.Path;

public final class Demucs {

    public static void demucs(ProcRunner runner, ODemucs options, Path audio, Path outputDir) throws IOException, InterruptedException {
        runner.runPythonExe(
            "demucs",
            "--two-stems=vocals",
            "--shifts=" + options.shifts(),
            "--out=" + outputDir,
            audio.toString()
        );
    }
}
