package karaed.tools;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ProcRunner {

    public interface OutputCapture {

        void output(boolean stderr, String text);
    }

    private final Tools tools;
    private final Path rootDir;
    private final OutputCapture output;

    public ProcRunner(Tools tools, Path rootDir, OutputCapture output) {
        this.tools = tools;
        this.rootDir = rootDir;
        this.output = output;
    }

    public void log(boolean stderr, String text) {
        output.output(stderr, text);
    }

    public void println(String line) {
        log(false, line + System.lineSeparator());
    }

    private static Path exe(Path dir, String name) {
        return dir == null ? Path.of(name) : dir.resolve(name);
    }

    private void capture(Reader rdr, boolean stderr) {
        Thread thread = new Thread(() -> {
            try {
                char[] buf = new char[16_384];
                while (true) {
                    int read = rdr.read(buf);
                    if (read < 0)
                        break;
                    String text = new String(buf, 0, read);
                    output.output(stderr, text);
                }
            } catch (IOException ex) {
                // ignore
            }
        });
        thread.start();
    }

    private void runCommand(String what, Path exe, List<String> args, Consumer<Reader> out) throws IOException, InterruptedException {
        List<Path> pathDirs;
        if (tools.ffmpegDir != null) {
            pathDirs = Collections.singletonList(tools.ffmpegDir);
        } else {
            pathDirs = Collections.emptyList();
        }
        Consumer<Reader> stdout;
        Consumer<Reader> stderr;
        if (out != null) {
            stderr = ProcUtil::eatOutput;
            stdout = out;
        } else {
            stderr = rdr -> capture(rdr, true);
            stdout = rdr -> capture(rdr, false);
        }
        ProcUtil.runCommand(
            what, exe, args, pathDirs,
            stdout, stderr, str -> output.output(true, "\n" + str + "\n")
        );
    }

    public <T> T runPythonScript(String script, Function<Reader, T> parseStdout, String... args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(rootDir.resolve(script).toString());
        list.addAll(Arrays.asList(args));
        ParseCapture<T> capture = new ParseCapture<>(parseStdout);
        runCommand("script " + script, exe(tools.pythonDir, "python"), list, capture);
        return capture.getParsed();
    }

    public void runPythonScript(String script, String... args) throws IOException, InterruptedException {
        runPythonScript(script, null, args);
    }

    public void runPythonExe(String exe, String... args) throws IOException, InterruptedException {
        runCommand(exe, exe(tools.pythonExeDir, exe), List.of(args), null);
    }

    private void runFF(String ff, List<String> args, Consumer<Reader> out) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.addAll(List.of("-v", "quiet"));
        list.addAll(args);
        runCommand(ff, exe(tools.ffmpegDir, ff), list, out);
    }

    public void runFFMPEG(List<String> args) throws IOException, InterruptedException {
        runFF("ffmpeg", args, null);
    }

    public void runFFProbeStreaming(List<String> args, Consumer<Reader> out) throws IOException, InterruptedException {
        runFF("ffprobe", args, out);
    }

    public <T> T runFFProbe(List<String> args, Function<Reader, T> parseStdout) throws IOException, InterruptedException {
        ParseCapture<T> capture = new ParseCapture<>(parseStdout);
        runFFProbeStreaming(args, capture);
        return capture.getParsed();
    }

    private static final class ParseCapture<T> implements Consumer<Reader> {

        private final Function<Reader, T> parser;
        private T parsed = null;

        ParseCapture(Function<Reader, T> parser) {
            this.parser = parser;
        }

        @Override
        public void accept(Reader stdout) {
            if (parser != null) {
                parsed = parser.apply(stdout);
            }
        }

        T getParsed() {
            return parsed;
        }
    }
}
