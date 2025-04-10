package karaed.tools;

import java.nio.file.Path;

public final class Tools {

    public final Path pythonDir;
    public final Path pythonExeDir;
    public final Path ffmpegDir;

    public Tools(Path pythonDir, Path ffmpegDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonDir == null ? null : pythonDir.resolve("Scripts");
        this.ffmpegDir = ffmpegDir;
    }
}
