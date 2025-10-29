package karaed.tools;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProcRunner<T> {

    private final Tools tools;
    private final Path rootDir;
    private final OutputCapture output;
    private final OutputProcessor<T> parseStdout;

    ProcRunner(Tools tools, Path rootDir, OutputCapture output, OutputProcessor<T> parseStdout) {
        this.tools = tools;
        this.rootDir = rootDir;
        this.output = output;
        this.parseStdout = parseStdout;
    }

    private T capture(Reader rdr, boolean stderr) throws IOException {
        char[] buf = new char[16_384];
        while (true) {
            int read = rdr.read(buf);
            if (read < 0)
                break;
            String text = new String(buf, 0, read);
            output.output(stderr, text);
        }
        return null;
    }

    private T runCommand(String what, Path exe, List<String> args) throws IOException, InterruptedException {
        List<Path> pathDirs = tools.ffmpegDirs();
        OutputProcessor<T> stdout;
        OutputProcessor<Object> stderr;
        if (parseStdout != null) {
            stdout = parseStdout;
            stderr = stream -> stream.transferTo(Writer.nullWriter());
        } else {
            stdout = rdr -> capture(rdr, false);
            stderr = rdr -> capture(rdr, true);
        }
        Pair<T, Object> pair = ProcUtil.runCommand(
            what, exe, args, pathDirs,
            stdout, stderr, str -> output.output(true, "\n" + str + "\n")
        );
        return pair.stdout();
    }

    public T python(String what, List<String> args) throws IOException, InterruptedException {
        return runCommand(what, tools.python(), args);
    }

    public T python(String what, String... args) throws IOException, InterruptedException {
        return python(what, List.of(args));
    }

    public T pythonScript(String script, String... args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(rootDir.resolve(script).toString());
        list.addAll(List.of(args));
        return python("script " + script, list);
    }

    public T pythonTool(String exe, List<String> args) throws IOException, InterruptedException {
        return runCommand(exe, tools.pythonTool(exe), args);
    }

    public T pythonTool(String exe, String... args) throws IOException, InterruptedException {
        return pythonTool(exe, List.of(args));
    }

    private T runFF(String ff, List<String> args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.addAll(List.of("-v", "quiet"));
        list.addAll(args);
        return runCommand(ff, tools.ffmpegTool(ff), list);
    }

    public void ffmpeg(List<String> args) throws IOException, InterruptedException {
        runFF("ffmpeg", args);
    }

    public T ffprobe(String... args) throws IOException, InterruptedException {
        return runFF("ffprobe", List.of(args));
    }
}
