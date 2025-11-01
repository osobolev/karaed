package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

final class RunUpdate {

    private final SetupTools tools;
    private final SoftSources sources;
    private final ToolRunner runner;

    RunUpdate(SetupTools tools, SoftSources sources, ToolRunner runner) {
        this.tools = tools;
        this.sources = sources;
        this.runner = runner;
    }

    void update(Tool tool) throws IOException, InterruptedException {
        switch (tool) {
        case PYTHON:
            throw new IllegalArgumentException("Cannot update " + tool);
        case FFMPEG:
            updateFFMPEG();
            break;
        case PIP:
            updatePIP();
            break;
        default:
            updatePackage(tool);
            break;
        }
    }

    private void updatePIP() throws IOException, InterruptedException {
        runner.run().python("pip update", "-m", "pip", "install", "--upgrade", "pip");
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
        new InstallRunner(tools, sources, runner).installFFMPEG();
    }
}
