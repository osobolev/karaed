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
import java.util.ArrayList;
import java.util.List;

public final class Main {

    private record Args(String rootDir, List<String> args) {}

    private static Args parseArgs(String[] args) {
        String rootDir = null;
        List<String> other = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            if (arg.startsWith("-")) {
                String option = arg.substring(1);
                if ("r".equals(option)) {
                    if (i < args.length) {
                        rootDir = args[i++];
                    }
                }
            } else {
                other.add(arg);
            }
        }
        return new Args(rootDir, other);
    }

    public static void main(String[] args) {
        ProcUtil.registerShutdown();

        ErrorLogger logger = new FileLogger("karaed.log");
        // todo:
        Tools tools = new Tools(
            Path.of("C:\\Users\\sobol\\.jkara\\python"),
            Path.of("C:\\Users\\sobol\\.jkara\\ffmpeg\\bin")
        );

        Args pargs = parseArgs(args);
        SwingUtilities.invokeLater(() -> {
            if (pargs.rootDir == null) {
                ShowMessage.error(null, "No root directory specified");
                return;
            }
            Path rootDir = Path.of(pargs.rootDir);
            Thread.currentThread().setUncaughtExceptionHandler((t, ex) -> logger.error(ex));
            if (!pargs.args.isEmpty()) {
                Path path = Path.of(pargs.args.getFirst());
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
