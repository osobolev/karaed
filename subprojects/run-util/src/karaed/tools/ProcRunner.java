package karaed.tools;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ProcRunner {

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

    private <T> T capture(Reader rdr, boolean stderr) throws IOException {
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

    private <T> T runCommand(String what, Path exe, List<String> args, OutputProcessor<T> out) throws IOException, InterruptedException {
        List<Path> pathDirs = tools.ffmpegDirs();
        OutputProcessor<T> stdout;
        OutputProcessor<Object> stderr;
        if (out != null) {
            stdout = out;
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

    public <T> T runPython(String what, List<String> args, OutputProcessor<T> parseStdout) throws IOException, InterruptedException {
        OutputProcessor<T> out;
        if (parseStdout != null) {
            out = parseStdout;
        } else {
            out = stream -> null;
        }
        return runCommand(what, tools.python(), args, out);
    }

    public <T> T runPythonScript(String script, OutputProcessor<T> parseStdout, String... args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(rootDir.resolve(script).toString());
        list.addAll(Arrays.asList(args));
        return runPython("script " + script, list, parseStdout);
    }

    public <T> T runPythonExe(String exe, OutputProcessor<T> parseStdout, String... args) throws IOException, InterruptedException {
        return runCommand(exe, tools.pythonTool(exe), List.of(args), parseStdout);
    }

    private <T> T runFF(String ff, List<String> args, OutputProcessor<T> parseStdout) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.addAll(List.of("-v", "quiet"));
        list.addAll(args);
        return runCommand(ff, tools.ffmpegTool(ff), list, parseStdout);
    }

    public void runFFMPEG(List<String> args) throws IOException, InterruptedException {
        runFF("ffmpeg", args, null);
    }

    public <T> T runFFProbe(List<String> args, OutputProcessor<T> parseStdout) throws IOException, InterruptedException {
        return runFF("ffprobe", args, parseStdout);
    }

    public static void registerShutdown() {
        ProcUtil.registerShutdown();
    }
}
