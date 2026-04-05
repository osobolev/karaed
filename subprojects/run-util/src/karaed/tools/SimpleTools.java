package karaed.tools;

import java.nio.file.Path;

public record SimpleTools(
    Path pythonDir,
    Path pythonExeDir,
    Path ffmpegBinDir
) implements Tools {
}
