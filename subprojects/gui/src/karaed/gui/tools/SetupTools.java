package karaed.gui.tools;

import karaed.tools.Tools;

import java.nio.file.Files;
import java.nio.file.Path;

final class SetupTools extends Tools {

    final SoftSources sources;

    SetupTools(Tools tools, SoftSources sources) {
        super(tools);
        this.sources = sources;
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
        return switch (tool) {
            case PYTHON -> python();
            case FFMPEG -> ffmpegTool("ffprobe");
            case PIP -> pythonTool("pip");
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
