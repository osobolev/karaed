package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;
import java.util.*;

abstract class InstallRunner {

    private final SetupTools tools;
    final ToolRunner runner;

    InstallRunner(SetupTools tools, ToolRunner runner) {
        this.tools = tools;
        this.runner = runner;
    }

    final void log(String message) {
        runner.println(message);
    }

    final void require(Tool... tools) {
        for (Tool tool : tools) {
            if (!this.tools.installed(tool))
                throw new IllegalStateException(tool + " is not installed!");
        }
    }

    private void installPackages(Collection<Tool> toInstall) throws IOException, InterruptedException {
        log("Installing required packages...");
        List<String> args = new ArrayList<>(List.of("-v", "install", "--no-warn-script-location"));
        for (Tool tool : toInstall) {
            args.addAll(tool.additionalPacks());
            args.add(tool.packName());
        }
        runner.run().pythonTool("pip", args);
    }

    final void installMissingPackages(Set<Tool> tools) throws IOException, InterruptedException {
        Set<Tool> packs = EnumSet.noneOf(Tool.class);
        for (Tool tool : tools) {
            String packName = tool.maybePackName();
            if (packName != null) {
                packs.add(tool);
            }
        }
        if (!packs.isEmpty()) {
            require(Tool.PYTHON, Tool.PIP);
            installPackages(packs);
        }
    }
}
