package karaed.gui;

import karaed.gui.options.OptionsDialog;
import karaed.gui.project.ProjectFrame;
import karaed.gui.start.RecentItems;
import karaed.gui.start.StartFrame;
import karaed.gui.util.ShowMessage;
import karaed.project.Workdir;
import karaed.tools.ProcUtil;
import karaed.tools.Tools;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Main {

    private record Args(String rootDir, boolean create, boolean help, List<String> paths, List<URI> uris) {

        Path getProjectDir() {
            if (paths.isEmpty())
                return Path.of(System.getProperty("user.dir"));
            Path path = Path.of(paths.getFirst());
            if (Files.isDirectory(path)) {
                return path;
            } else {
                return path.toAbsolutePath().getParent();
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
        boolean help = false;
        List<String> paths = new ArrayList<>();
        List<URI> uris = new ArrayList<>();
        int i = 0;
        while (i < args.length) {
            String arg = args[i++];
            String option;
            if (arg.startsWith("--")) {
                option = arg.substring(2);
            } else if (arg.startsWith("-")) {
                option = arg.substring(1);
            } else {
                option = null;
            }
            if (option != null) {
                if ("r".equals(option)) {
                    if (i < args.length) {
                        rootDir = args[i++];
                    }
                } else if ("new".equals(option) || "create".equals(option)) {
                    create = true;
                } else if ("help".equals(option)) {
                    help = true;
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
        return new Args(rootDir, create, help, paths, uris);
    }

    private static void help() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("Usage:");
        pw.println("karaed -new [<dir>]");
        pw.println("karaed -create [<dir>]");
        pw.println("    - creates new project in the directory (current directory by default)");
        pw.println("karaed <URL> [<dir>]");
        pw.println("    - creates new project in the directory (current directory by default) with URL as a source");
        pw.println("karaed <path>");
        pw.println("    - opens the project (path can be a project directory or any file in it)");
        pw.println("karaed");
        pw.println("    - opens project list window");
        pw.close();
        JOptionPane.showMessageDialog(null, sw.toString(), "Help", JOptionPane.INFORMATION_MESSAGE);
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
            Thread.currentThread().setUncaughtExceptionHandler((t, ex) -> logger.error(ex));
            if (pargs.help) {
                help();
                return;
            }
            if (pargs.rootDir == null) {
                ShowMessage.error(null, "No root directory specified");
                return;
            }
            Path rootDir = Path.of(pargs.rootDir);
            Workdir workDir;
            if (!pargs.paths.isEmpty()) {
                workDir = new Workdir(pargs.getProjectDir());
            } else if (pargs.create || !pargs.uris.isEmpty()) {
                Path dir = pargs.getProjectDir();
                Workdir existingWorkDir = new Workdir(dir);
                boolean openExisting;
                if (RecentItems.isProjectDir(existingWorkDir) == null) {
                    int ans = JOptionPane.showConfirmDialog(
                        null, "Project already exists. Open it?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
                    );
                    if (ans == JOptionPane.YES_OPTION) {
                        openExisting = true;
                    } else if (ans == JOptionPane.NO_OPTION) {
                        openExisting = false;
                    } else {
                        return;
                    }
                } else {
                    openExisting = false;
                }
                if (openExisting) {
                    workDir = existingWorkDir;
                } else {
                    OptionsDialog dlg;
                    try {
                        dlg = OptionsDialog.newProject(logger, null, dir, pargs.getDefaultURL());
                    } catch (Exception ex) {
                        ShowMessage.error(logger, null, ex);
                        return;
                    }
                    if (!dlg.isSaved())
                        return;
                    workDir = dlg.getWorkDir();
                }
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
