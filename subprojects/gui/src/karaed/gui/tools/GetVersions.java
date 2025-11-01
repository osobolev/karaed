package karaed.gui.tools;

import karaed.gui.tools.formats.ffprobe.FFVersion;
import karaed.json.JsonUtil;
import karaed.tools.OutputProcessor;
import karaed.tools.ToolRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GetVersions {

    private final SetupTools tools;
    private final ToolRunner runner;

    GetVersions(SetupTools tools, ToolRunner runner) {
        this.tools = tools;
        this.runner = runner;
    }

    String getVersion(Tool tool) throws IOException, InterruptedException {
        return switch (tool) {
            case PYTHON -> pythonVersion();
            case FFMPEG -> ffmpegVersion();
            default -> packageVersion(tool);
        };
    }

    private String pythonVersion() throws IOException, InterruptedException {
        if (!tools.installed(Tool.PYTHON))
            return null;
        OutputProcessor<String> parseVersion = stdout -> {
            String version = null;
            BufferedReader br = new BufferedReader(stdout);
            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;
                if (version == null) {
                    version = line.trim();
                }
            }
            return version;
        };
        return runner.run(parseVersion).python("version", "--version");
    }

    private String packageVersion(Tool tool) throws IOException, InterruptedException {
        if (!tools.installed(Tool.PIP))
            return null;
        OutputProcessor<String> parseVersion = stdout -> {
            BufferedReader br = new BufferedReader(stdout);
            String version = null;
            Pattern pattern = Pattern.compile("Version:\\s*(.+)");
            while (true) {
                String line = br.readLine();
                if (line == null)
                    break;
                Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    version = matcher.group(1).trim();
                }
            }
            return version;
        };
        return runner.run(parseVersion).pythonTool("pip", "show", tool.packName());
    }

    private String ffmpegVersion() throws IOException, InterruptedException {
        if (!tools.installed(Tool.FFMPEG))
            return null;
        FFVersion ff = runner.run(JsonUtil.parser(FFVersion.class)).ffprobe(
            "-v", "quiet",
            "-output_format", "json",
            "-show_program_version"
        );
        return ff.programVersion().version();
    }
}
