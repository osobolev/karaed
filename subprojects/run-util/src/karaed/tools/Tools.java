package karaed.tools;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public final class Tools {

    private final Path pythonDir;
    private final Path pythonExeDir;
    private final Path ffmpegBinDir;

    public Tools(Path pythonDir, Path pythonExeDir, Path ffmpegBinDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonExeDir;
        this.ffmpegBinDir = ffmpegBinDir;
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
