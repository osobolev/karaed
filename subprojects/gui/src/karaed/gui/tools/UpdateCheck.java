package karaed.gui.tools;

import karaed.gui.tools.formats.pip.PipVersions;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class UpdateCheck {

    private final SoftSources sources;
    private final ToolRunner runner;

    UpdateCheck(SoftSources sources, ToolRunner runner) {
        this.sources = sources;
        this.runner = runner;
    }

    String checkForUpdate(Tool tool) throws IOException, InterruptedException {
        runner.println("Checking for update of " + tool + "...");
        return switch (tool) {
            case PYTHON -> throw new IllegalArgumentException("Python cannot be updated");
            case FFMPEG -> checkFFMPEGUpdate();
            default -> checkPackageUpdate(tool);
        };
    }

    private String checkPackageUpdate(Tool tool) throws IOException, InterruptedException {
        PipVersions versions = runner.run(JsonUtil.parser(PipVersions.class)).pythonTool(
            "pip",
            "index", "versions", tool.packName(), "--json"
        );
        return versions.latest();
    }

    private String checkFFMPEG(String urlPattern, String versionURL) throws IOException {
        String[] version = new String[1];
        if (sources.ffmpegUrl().matches(urlPattern)) {
            Download.download(versionURL, is -> {
                version[0] = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            });
        }
        return version[0];
    }

    private String checkFFMPEGUpdate() throws IOException {
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
