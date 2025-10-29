package karaed.tools;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class Tools {

    private final Path pythonDir;
    private final Path pythonExeDir;
    private final Path ffmpegDir;

    public Tools(Path pythonDir, Path ffmpegDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonDir == null ? null : pythonDir.resolve("Scripts");
        this.ffmpegDir = ffmpegDir;
    }

    public static Tools create(Path installDir) {
        return new Tools(
            installDir.resolve("python"),
            installDir.resolve("ffmpeg/bin")
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

    public Path ffmpegTool(String tool) {
        return exe(ffmpegDir, tool);
    }

    public List<Path> ffmpegDirs() {
        if (ffmpegDir != null) {
            return Collections.singletonList(ffmpegDir);
        } else {
            return Collections.emptyList();
        }
    }
}
