package karaed.gui;

import karaed.gui.project.ProjectFrame;
import karaed.tools.ProcUtil;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.swing.*;
import java.nio.file.Path;

public final class Main {

    public static void main(String[] args) {
        ProcUtil.registerShutdown();
        SwingUtilities.invokeLater(() -> {
            // todo:
            Tools tools = new Tools(
                Path.of("C:\\Users\\sobol\\.jkara\\python"),
                Path.of("C:\\Users\\sobol\\.jkara\\ffmpeg\\bin")
            );
            new ProjectFrame(
                new FileLogger("karaed.log"), tools,
                Path.of("C:\\home\\projects\\my\\kara2"),
                new Workdir(Path.of("C:\\home\\projects\\my\\kara2\\work\\test"))
            );
        });
    }
}
