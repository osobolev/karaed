package karaed.gui.tools;

import karaed.gui.ErrorLogger;
import karaed.gui.tools.formats.pip.PipVersions;
import karaed.json.JsonUtil;
import karaed.tools.OutputCapture;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ToolActions {

    private final ErrorLogger logger;
    private final SetupTools tools;
    private final ToolRunner runner;

    ToolActions(ErrorLogger logger, SetupTools tools, OutputCapture output) {
        this.logger = logger;
        this.tools = tools;
        this.runner = new ToolRunner(tools, null, output);
    }

    private void error(Throwable ex) {
        logger.error(ex);
        runner.println(ex.toString());
    }

    Map<Tool, String> getInstalledVersions(Iterable<Tool> tools) {
        GetVersions getVersions = new GetVersions(this.tools, runner);
        Map<Tool, String> versions = new EnumMap<>(Tool.class);
        for (Tool tool : tools) {
            runner.println("Get current version for " + tool + "...");
            String version = null;
            try {
                version = getVersions.getVersion(tool);
            } catch (IOException ex) {
                error(ex);
            } catch (InterruptedException ex) {
                break;
            }
            versions.put(tool, version);
        }
        return versions;
    }

    Map<Tool, String> checkForUpdates() {
        Map<Tool, String> newVersions = new EnumMap<>(Tool.class);
        for (Tool tool : List.of(Tool.PIP, Tool.YT_DLP, Tool.DEMUCS, Tool.WHISPERX)) {
            try {
                runner.println("Checking update for " + tool + "...");
                PipVersions versions = runner.run(JsonUtil.parser(PipVersions.class)).pythonTool(
                    "pip",
                    "index", "versions", tool.packName(), "--json"
                );
                newVersions.put(tool, versions.latest());
            } catch (IOException ex) {
                error(ex);
            } catch (InterruptedException ex) {
                break;
            }
        }
        // todo: check for ffmpeg updates??? need essential builds for that!!!!!!
        return newVersions;
    }

    Map<Tool, String> update(Tool tool) {
        try {
            new RunUpdate(tools, runner).update(tool);
        } catch (IOException ex) {
            error(ex);
        } catch (InterruptedException ex) {
            return null;
        }
        return getInstalledVersions(List.of(tool));
    }

    Map<Tool, String> installMissing(Set<Tool> tools) {
        try {
            new InstallRunner(this.tools, runner).install(tools);
        } catch (IOException ex) {
            error(ex);
        } catch (InterruptedException ex) {
            return null;
        }
        return getInstalledVersions(tools);
    }
}
