package karaed.gui.tools;

import karaed.tools.Tools;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class SetupTools {

    static Path appDir() {
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, ".karaed");
    }

    public static SetupTools create() {
        return WindowsSetupTools.create();
    }

    public abstract Tools toTools();

    private Path toolPath(Tool tool) {
        Tools tools = toTools();
        return switch (tool) {
            case PYTHON -> tools.python();
            case FFMPEG -> tools.ffmpegTool("ffprobe");
            case PIP -> tools.pythonTool("pip");
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
