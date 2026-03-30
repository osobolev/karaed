package karaed.tools;

import java.util.function.IntPredicate;

public final class ToolRunner {

    private static final IntPredicate IS_SUCCESS = exitCode -> exitCode == 0;

    private final Tools tools;
    private final OutputCapture output;

    public ToolRunner(Tools tools, OutputCapture output) {
        this.tools = tools;
        this.output = output;
    }

    public void log(boolean stderr, String text) {
        output.output(stderr, text);
    }

    public void println(String line) {
        log(false, line + System.lineSeparator());
    }

    public <T> ProcRunner<T> run(OutputProcessor<T> parseStdout, IntPredicate isOK) {
        return new ProcRunner<>(tools, output, parseStdout, isOK);
    }

    public <T> ProcRunner<T> run(OutputProcessor<T> parseStdout) {
        return run(parseStdout, IS_SUCCESS);
    }

    public ProcRunner<Object> run() {
        return run(null);
    }

    public static void registerShutdown() {
        ProcUtil.registerShutdown();
    }
}
