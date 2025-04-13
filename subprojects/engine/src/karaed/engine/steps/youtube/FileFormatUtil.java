package karaed.engine.steps.youtube;

import karaed.engine.formats.ffprobe.FFFormat;
import karaed.engine.formats.ffprobe.FFFormatOutput;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class FileFormatUtil {

    static FFFormat getFormat(ProcRunner runner, Path file) throws IOException, InterruptedException {
        FFFormatOutput format = runner.runFFProbe(
            List.of(
                "-print_format", "json",
                "-show_entries", "format",
                file.toString()
            ),
            rdr -> JsonUtil.parse(rdr, FFFormatOutput.class)
        );
        return format.format();
    }
}
