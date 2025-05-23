package karaed.gui;

import karaed.gui.project.ProjectFrame;
import karaed.gui.start.StartFrame;
import karaed.gui.util.ShowMessage;
import karaed.project.Workdir;
import karaed.tools.ProcUtil;
import karaed.tools.Tools;

import javax.swing.*;
import java.nio.file.Files;
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
        Path rootDir = Path.of("C:\\home\\projects\\my\\karaed");

        SwingUtilities.invokeLater(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t, ex) -> logger.error(ex));
            if (args.length > 0) {
                Path path = Path.of(args[0]);
                Path dir;
                if (Files.isDirectory(path)) {
                    dir = path;
                } else {
                    dir = path.getParent();
                }
                ProjectFrame pf = ProjectFrame.create(
                    logger, false, tools, rootDir, new Workdir(dir),
                    error -> ShowMessage.error(null, error)
                );
                if (pf != null) {
                    pf.setVisible(true);
                }
            } else {
                new StartFrame(logger, tools, rootDir);
            }
        });
    }
}
