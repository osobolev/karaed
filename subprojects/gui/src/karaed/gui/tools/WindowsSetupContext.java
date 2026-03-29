package karaed.gui.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static karaed.gui.tools.Download.download;

final class WindowsSetupContext extends SetupContext {

    final WindowsSetupTools wintools;
    final SoftSources sources;

    WindowsSetupContext(WindowsSetupTools tools, SoftSources sources) {
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
}
