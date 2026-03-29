package karaed.gui.tools;

import karaed.tools.Tools;

import java.nio.file.Path;

public abstract class SetupTools {

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

    final boolean installed(Tool tool) {
        Path path = toolPath(tool);
        return exeExists(path);
    }

    abstract boolean exeExists(Path toolPath);
}
