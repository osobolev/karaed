package karaed.gui;

import karaed.gui.options.OptionsDialog;
import karaed.gui.project.ProjectFrame;
import karaed.gui.start.StartFrame;
import karaed.gui.util.ShowMessage;
import karaed.project.Workdir;
import karaed.tools.ProcUtil;
import karaed.tools.Tools;

import javax.swing.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Main {

    private record Args(String rootDir, boolean create, List<String> paths, List<URI> uris) {

        Path getProjectDir() {
            if (paths.isEmpty())
                return Path.of(System.getProperty("user.dir"));
            Path path = Path.of(paths.getFirst());
            if (Files.isDirectory(path)) {
                return path;
            } else {
                return path.getParent();
            }
        }

        String getDefaultURL() {
            if (uris.isEmpty())
                return null;
            return uris.getFirst().toString();
        }
    }

    private static Args parseArgs(String[] args) {
        String rootDir = null;
        boolean create = false;
        List<String> paths = new ArrayList<>();
        List<URI> uris = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            if (arg.startsWith("-")) {
                String option = arg.substring(1);
                if ("r".equals(option)) {
                    if (i < args.length) {
                        rootDir = args[i++];
                    }
                } else if ("new".equals(option) || "create".equals(option)) {
                    create = true;
                }
            } else {
                URI uri = null;
                try {
                    uri = new URI(arg);
                } catch (Exception ex) {
                    // ignore
                }
                if (uri != null && uri.getScheme() != null && uri.getScheme().startsWith("http")) {
                    uris.add(uri);
                } else {
                    paths.add(arg);
                }
            }
        }
        return new Args(rootDir, create, paths, uris);
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
            Workdir workDir;
            if (!pargs.paths.isEmpty()) {
                workDir = new Workdir(pargs.getProjectDir());
            } else if (pargs.create || !pargs.uris.isEmpty()) {
                OptionsDialog dlg;
                try {
                    dlg = new OptionsDialog(
                        logger, "New project", null, null,
                        pargs.getProjectDir(), pargs.getDefaultURL()
                    );
                } catch (Exception ex) {
                    ShowMessage.error(logger, null, ex);
                    return;
                }
                if (!dlg.isSaved())
                    return;
                workDir = dlg.getWorkDir();
            } else {
                workDir = null;
            }
            if (workDir != null) {
                ProjectFrame pf = ProjectFrame.create(
                    logger, false, tools, rootDir, workDir,
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
