package karaed.engine.steps.demucs;

import karaed.engine.opts.ODemucs;
import karaed.tools.ProcRunner;

import java.io.IOException;
import java.nio.file.Path;

public final class Demucs {

    public static void demucs(ProcRunner runner, Path audio, ODemucs options, Path outputDir) throws IOException, InterruptedException {
        runner.println("Separating vocals and instrumental");
        runner.runPythonExe(
            "demucs",
            "--two-stems=vocals",
            "--shifts=" + options.shifts(),
            "--out=" + outputDir,
            audio.toString()
        );
    }
}
