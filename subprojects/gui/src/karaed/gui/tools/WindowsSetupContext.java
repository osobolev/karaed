package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static karaed.gui.tools.Download.download;

final class WindowsSetupContext extends SetupContext {

    final WindowsSetupTools wintools;
    final WindowsSoftSources sources;

    WindowsSetupContext(WindowsSetupTools tools, WindowsSoftSources sources) {
        super(tools);
        this.wintools = tools;
        this.sources = sources;
    }

    private String checkFFMPEG(String urlPattern, String versionURL) throws IOException, InterruptedException {
        String[] version = new String[1];
        if (sources.ffmpegUrl().matches(urlPattern)) {
            download(versionURL, null, is -> version[0] = new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
        return version[0];
    }

    @Override
    String checkFFMPEGUpdate() throws IOException, InterruptedException {
        return checkFFMPEG(
            "https?://www\\.gyan\\.dev/ffmpeg/builds/ffmpeg-release-essentials\\.zip",
            "https://www.gyan.dev/ffmpeg/builds/release-version"
        );
//        return checkFFMPEG(
//            "https?://www\\.gyan\\.dev/ffmpeg/builds/ffmpeg-git-essentials\\.7z", // todo: handle 7z!!!
//            "https://www.gyan.dev/ffmpeg/builds/git-version"
//        );
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

    @Override
    void updateFFMPEG(ToolRunner runner) throws IOException, InterruptedException {
        deleteDir(wintools.ffmpegDir());
        installRunner(runner).installFFMPEG();
    }

    @Override
    WindowsInstallRunner installRunner(ToolRunner runner) {
        return new WindowsInstallRunner(this, runner);
    }
}
