package karaed.engine.video;

import karaed.engine.formats.ffprobe.FFStream;
import karaed.engine.formats.ffprobe.FFStreams;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class FileStreamUtil {

    public static List<FFStream> listVideoStreams(ToolRunner runner, Path file) throws IOException, InterruptedException {
        FFStreams streams = runner.run(JsonUtil.parser(FFStreams.class)).ffprobe(
            "-print_format", "json",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height",
            file.toString()
        );
        return streams.streams();
    }
}
