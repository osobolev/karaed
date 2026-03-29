package karaed.gui.tools;

import karaed.tools.Tools;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WindowsSetupTools extends SetupTools {

    private final Path pythonDir;
    private final Path pythonExeDir;
    private final Path ffmpegDir;

    private WindowsSetupTools(Path pythonDir, Path pythonExeDir, Path ffmpegDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonExeDir;
        this.ffmpegDir = ffmpegDir;
    }

    public static WindowsSetupTools create(Path installDir) {
        Path pythonDir = installDir.resolve("python");
        Path pythonExeDir = pythonDir.resolve("Scripts");
        return new WindowsSetupTools(
            pythonDir, pythonExeDir,
            installDir.resolve("ffmpeg")
        );
    }

    public static WindowsSetupTools create(String folder) {
        String userHome = System.getProperty("user.home");
        return create(Path.of(userHome, folder));
    }

    public static WindowsSetupTools create() {
        return create(".karaed");
    }

    @Override
    public Tools toTools() {
        Path ffmpegBinDir = ffmpegDir == null ? null : ffmpegDir.resolve("bin");
        return new Tools(pythonDir, pythonExeDir, ffmpegBinDir);
    }

    @Override
    boolean exeExists(Path toolPath) {
        Path win = toolPath.resolveSibling(toolPath.getFileName() + ".exe");
        return Files.exists(win);
    }

    Path pythonDir() {
        return pythonDir;
    }

    Path ffmpegDir() {
        return ffmpegDir;
    }
}
