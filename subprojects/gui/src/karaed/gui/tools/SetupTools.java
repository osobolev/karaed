package karaed.gui.tools;

import karaed.tools.Tools;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SetupTools {

    private final Path pythonDir;
    private final Path pythonExeDir;
    private final Path ffmpegDir;

    private SetupTools(Path pythonDir, Path pythonExeDir, Path ffmpegDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonExeDir;
        this.ffmpegDir = ffmpegDir;
    }

    public static SetupTools create(Path installDir) {
        Path pythonDir = installDir.resolve("python");
        Path pythonExeDir = pythonDir.resolve("Scripts");
        return new SetupTools(
            pythonDir, pythonExeDir,
            installDir.resolve("ffmpeg")
        );
    }

    public static SetupTools create(String folder) {
        String userHome = System.getProperty("user.home");
        return create(Path.of(userHome, folder));
    }

    public static SetupTools create() {
        return create(".karaed");
    }

    public Tools toTools() {
        Path ffmpegBinDir = ffmpegDir == null ? null : ffmpegDir.resolve("bin");
        return new Tools(pythonDir, pythonExeDir, ffmpegBinDir);
    }

    private static boolean exeExists(Path exe) {
        if (Files.exists(exe))
            return true;
        Path win = exe.resolveSibling(exe.getFileName() + ".exe");
        if (Files.exists(win))
            return true;
        return false;
    }

    private Path toolPath(Tool tool) {
        Tools tools = toTools();
        return switch (tool) {
            case PYTHON -> tools.python();
            case FFMPEG -> tools.ffmpegTool("ffprobe");
            case PIP -> tools.pythonTool("pip");
            default -> throw new IllegalArgumentException("Tool " + tool + " is a Python package");
        };
    }

    boolean installed(Tool tool) {
        Path path = toolPath(tool);
        return exeExists(path);
    }

    Path pythonDir() {
        return pythonDir;
    }

    Path ffmpegDir() {
        return ffmpegDir;
    }
}
