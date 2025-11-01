package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class RunUpdate {

    private final SetupTools tools;
    private final ToolRunner runner;

    RunUpdate(SetupTools tools, ToolRunner runner) {
        this.tools = tools;
        this.runner = runner;
    }

    void update(Tool tool) throws IOException, InterruptedException {
        switch (tool) {
        case PYTHON:
            throw new IllegalArgumentException("Cannot update " + tool);
        case FFMPEG:
            updateFFMPEG();
            break;
        default:
            updatePackage(tool);
            break;
        }
    }

    private void updatePackage(Tool tool) throws IOException, InterruptedException {
        runner.run().pythonTool("pip", "-v", "install", "--upgrade", tool.packName());
    }

    private static void deleteDir(Path dir) throws IOException {
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(dir)) {
            for (Path path : paths) {
                if (Files.isDirectory(path)) {
                    deleteDir(path);
                }
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    private void updateFFMPEG() throws IOException {
        deleteDir(tools.ffmpegDir());
        new InstallRunner(tools, runner).installFFMPEG();
    }
}
