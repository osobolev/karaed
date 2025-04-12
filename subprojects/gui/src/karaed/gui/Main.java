package karaed.gui;

import karaed.gui.project.ProjectFrame;
import karaed.gui.start.StartFrame;
import karaed.tools.ProcUtil;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.swing.*;
import java.nio.file.Path;

public final class Main {

    public static void main(String[] args) {
        ProcUtil.registerShutdown();

        ErrorLogger logger = new FileLogger("karaed.log");
        // todo:
        Tools tools = new Tools(
            Path.of("C:\\Users\\sobol\\.jkara\\python"),
            Path.of("C:\\Users\\sobol\\.jkara\\ffmpeg\\bin")
        );
        Path rootDir = Path.of("C:\\home\\projects\\my\\kara2");

        SwingUtilities.invokeLater(() -> {
            if (args.length > 0) {
                Path dir = Path.of(args[0]);
                // todo: check if really project dir
                new ProjectFrame(logger, tools, rootDir, new Workdir(dir));
            } else {
                new StartFrame(logger, tools, rootDir);
            }
        });
    }
}
