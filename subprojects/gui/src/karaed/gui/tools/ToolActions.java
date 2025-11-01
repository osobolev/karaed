package karaed.gui.tools;

import karaed.gui.ErrorLogger;
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
    private final SoftSources sources;
    private final ToolRunner runner;

    private boolean hasErrors = false;

    ToolActions(ErrorLogger logger, SetupTools tools, SoftSources sources, OutputCapture output) {
        this.logger = logger;
        this.tools = tools;
        this.sources = sources;
        this.runner = new ToolRunner(tools, null, output);
    }

    private void error(Throwable ex) {
        hasErrors = true;
        logger.error(ex);
        runner.println(ex.toString());
    }

    boolean hasErrors() {
        return hasErrors;
    }

    Map<Tool, String> getInstalledVersions(Iterable<Tool> tools) {
        GetVersions getVersions = new GetVersions(this.tools, runner);
        Map<Tool, String> versions = new EnumMap<>(Tool.class);
        for (Tool tool : tools) {
            String version = null;
            try {
                version = getVersions.getVersion(tool);
            } catch (IOException ex) {
                error(ex);
            } catch (InterruptedException ex) {
                return null;
            }
            versions.put(tool, version);
        }
        return versions;
    }

    Map<Tool, String> checkForUpdates() {
        UpdateCheck updateCheck = new UpdateCheck(sources, runner);
        Map<Tool, String> newVersions = new EnumMap<>(Tool.class);
        for (Tool tool : List.of(Tool.FFMPEG, Tool.PIP, Tool.YT_DLP, Tool.DEMUCS, Tool.WHISPERX)) {
            try {
                runner.println("Checking for update of " + tool + "...");
                String newVersion = updateCheck.checkForUpdate(tool);
                if (newVersion != null) {
                    newVersions.put(tool, newVersion);
                }
            } catch (IOException ex) {
                error(ex);
            } catch (InterruptedException ex) {
                break;
            }
        }
        return newVersions;
    }

    Map<Tool, String> update(Tool tool) {
        try {
            new RunUpdate(tools, sources, runner).update(tool);
        } catch (IOException ex) {
            error(ex);
        } catch (InterruptedException ex) {
            return null;
        }
        return getInstalledVersions(List.of(tool));
    }

    Map<Tool, String> installMissing(Set<Tool> tools) {
        try {
            new InstallRunner(this.tools, sources, runner).install(tools);
        } catch (IOException ex) {
            error(ex);
        } catch (InterruptedException ex) {
            return null;
        }
        return getInstalledVersions(tools);
    }
}
