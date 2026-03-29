package karaed.gui.tools;

import karaed.tools.SimpleTools;
import karaed.tools.Tools;

import java.nio.file.Files;
import java.nio.file.Path;

public final class WindowsSetupTools extends SetupTools {

    private final Path pythonDir;
    private final Path pythonExeDir;
    private final Path ffmpegDir;

    public WindowsSetupTools(Path pythonDir, Path pythonExeDir, Path ffmpegDir) {
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

    public static WindowsSetupTools create() {
        return create(appDir());
    }

    @Override
    public Tools toTools() {
        Path ffmpegBinDir = ffmpegDir == null ? null : ffmpegDir.resolve("bin");
        return new SimpleTools(pythonDir, pythonExeDir, ffmpegBinDir);
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
