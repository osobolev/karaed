package karaed.gui.tools;

import karaed.gui.tools.formats.pip.PipVersions;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import java.io.IOException;

final class UpdateCheck {

    private final SetupContext ctx;
    private final ToolRunner runner;

    UpdateCheck(SetupContext ctx, ToolRunner runner) {
        this.ctx = ctx;
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

    private String checkFFMPEGUpdate() throws IOException, InterruptedException {
        return ctx.checkFFMPEGUpdate();
    }
}
