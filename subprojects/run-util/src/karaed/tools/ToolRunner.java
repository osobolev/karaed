package karaed.tools;

import java.nio.file.Path;

public final class ToolRunner {

    private final Tools tools;
    private final Path rootDir;
    private final OutputCapture output;

    public ToolRunner(Tools tools, Path rootDir, OutputCapture output) {
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

    public ProcRunner<Object> run() {
        return new ProcRunner<>(tools, rootDir, output, null);
    }

    public <T> ProcRunner<T> run(OutputProcessor<T> parseStdout) {
        return new ProcRunner<>(tools, rootDir, output, parseStdout);
    }

    public static void registerShutdown() {
        ProcUtil.registerShutdown();
    }
}
