package karaed.gui.tools;

import karaed.tools.Tools;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class SetupTools implements Tools {

    public static Path appDir() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".karaed");
    }

    public static SetupTools create() {
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("win")) {
            return WindowsSetupTools.create();
        } else {
            return LinuxSetupTools.create();
        }
    }

    private Path toolPath(Tool tool) {
        return switch (tool) {
            case PYTHON -> python();
            case FFMPEG -> ffmpegTool("ffprobe");
            case PIP -> pythonTool("pip");
            default -> throw new IllegalArgumentException("Tool " + tool + " is a Python package");
        };
    }

    private boolean exeExists(Path exe) {
        if (Files.exists(exe))
            return true;
        Path win = exe.resolveSibling(exe.getFileName() + ".exe");
        if (Files.exists(win))
            return true;
        return false;
    }

    final boolean installed(Tool tool) {
        Path path = toolPath(tool);
        return exeExists(path);
    }
}
