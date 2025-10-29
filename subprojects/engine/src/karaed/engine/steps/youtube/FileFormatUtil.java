package karaed.engine.steps.youtube;

import karaed.engine.formats.ffprobe.FFFormat;
import karaed.engine.formats.ffprobe.FFFormatOutput;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.file.Path;

final class FileFormatUtil {

    static FFFormat getFormat(ToolRunner runner, Path file) throws IOException, InterruptedException {
        FFFormatOutput format = runner.run(stdout -> JsonUtil.parse(stdout, FFFormatOutput.class)).ffprobe(
            "-print_format", "json",
            "-show_entries", "format",
            file.toString()
        );
        return format.format();
    }
}
