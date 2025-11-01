package karaed.tools;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class Tools {

    protected final Path pythonDir;
    protected final Path pythonExeDir;
    protected final Path ffmpegDir;
    protected final Path ffmpegBinDir;

    public Tools(Path pythonDir, Path ffmpegDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonDir == null ? null : pythonDir.resolve("Scripts");
        this.ffmpegDir = ffmpegDir;
        this.ffmpegBinDir = ffmpegDir == null ? null : ffmpegDir.resolve("bin");
    }

    public Tools(Tools tools) {
        this.pythonDir = tools.pythonDir;
        this.pythonExeDir = tools.pythonExeDir;
        this.ffmpegDir = tools.ffmpegDir;
        this.ffmpegBinDir = tools.ffmpegBinDir;
    }

    public static Tools create(Path installDir) {
        return new Tools(
            installDir.resolve("python"),
            installDir.resolve("ffmpeg")
        );
    }

    public static Tools create() {
        String userHome = System.getProperty("user.home");
        return create(Path.of(userHome, ".jkara"));
    }

    private static Path exe(Path dir, String name) {
        return dir == null ? Path.of(name) : dir.resolve(name);
    }

    public final Path python() {
        return exe(pythonDir, "python");
    }

    public final Path pythonTool(String tool) {
        return exe(pythonExeDir, tool);
    }

    public final Path ffmpegTool(String tool) {
        return exe(ffmpegBinDir, tool);
    }

    public final List<Path> ffmpegDirs() {
        if (ffmpegBinDir != null) {
            return Collections.singletonList(ffmpegBinDir);
        } else {
            return Collections.emptyList();
        }
    }
}
