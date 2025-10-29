package karaed.tools;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class Tools {

    private final Path pythonDir;
    private final Path pythonExeDir;
    private final Path ffmpegDir;
    private final Path ffmpegBinDir;

    public Tools(Path pythonDir, Path ffmpegDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonDir == null ? null : pythonDir.resolve("Scripts");
        this.ffmpegDir = ffmpegDir;
        this.ffmpegBinDir = ffmpegDir == null ? null : ffmpegDir.resolve("bin");
    }

    public static Tools create(Path installDir) {
        return new Tools(
            installDir.resolve("python"),
            installDir.resolve("ffmpeg")
        );
    }

    private static Path exe(Path dir, String name) {
        return dir == null ? Path.of(name) : dir.resolve(name);
    }

    public Path python() {
        return exe(pythonDir, "python");
    }

    public Path pythonTool(String tool) {
        return exe(pythonExeDir, tool);
    }

    public Path ffmpegDir() {
        return ffmpegDir;
    }

    public Path ffmpegTool(String tool) {
        return exe(ffmpegBinDir, tool);
    }

    public List<Path> ffmpegDirs() {
        if (ffmpegBinDir != null) {
            return Collections.singletonList(ffmpegBinDir);
        } else {
            return Collections.emptyList();
        }
    }
}
