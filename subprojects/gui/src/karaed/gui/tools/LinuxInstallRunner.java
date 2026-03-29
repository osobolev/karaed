package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;
import java.util.Set;

final class LinuxInstallRunner extends InstallRunner {

    private final LinuxSetupContext ctx;

    LinuxInstallRunner(LinuxSetupContext ctx, ToolRunner runner) {
        super(ctx.tools, runner);
        this.ctx = ctx;
    }

    @Override
    void install(Set<Tool> tools) throws IOException, InterruptedException {
        if (tools.contains(Tool.PYTHON) || tools.contains(Tool.PIP) || tools.contains(Tool.FFMPEG)) {
            LinuxPathsDialog.requirePaths(ctx);
        }
        installMissingPackages(tools);
    }
}
