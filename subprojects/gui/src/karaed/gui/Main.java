package karaed.gui;

import karaed.gui.project.ProjectFrame;
import karaed.gui.start.StartFrame;
import karaed.tools.ProcUtil;
import karaed.tools.Tools;
import karaed.project.Workdir;

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
        Path rootDir = Path.of("C:\\home\\projects\\my\\kara2");

        SwingUtilities.invokeLater(() -> {
            if (args.length > 0) {
                Path path = Path.of(args[0]);
                Path dir;
                if (Files.isDirectory(path)) {
                    dir = path;
                } else {
                    dir = path.getParent();
                }
                ProjectFrame pf = ProjectFrame.create(logger, null, tools, rootDir, new Workdir(dir));
                if (pf != null) {
                    pf.setVisible(true);
                }
            } else {
                new StartFrame(logger, tools, rootDir);
            }
        });
    }
}
