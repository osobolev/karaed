package karaed.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public final class ProcRunner {

    private final Tools tools;
    private final Path rootDir;

    public ProcRunner(Tools tools, Path rootDir) {
        this.tools = tools;
        this.rootDir = rootDir;
    }

    private static Path exe(Path dir, String name) {
        return dir == null ? Path.of(name) : dir.resolve(name);
    }

    private void runCommand(String what, Path exe, List<String> args, Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
        List<Path> pathDirs;
        if (tools.ffmpegDir != null) {
            pathDirs = Collections.singletonList(tools.ffmpegDir);
        } else {
            pathDirs = Collections.emptyList();
        }
        ProcUtil.runCommand(what, exe, args, pathDirs, out, exitOk);
    }

    public void runPythonScript(String script, String... args) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.add(rootDir.resolve(script).toString());
        list.addAll(Arrays.asList(args));
        runCommand("script " + script, exe(tools.pythonDir, "python"), list, null, null);
    }

    public void runPythonExe(String exe, String... args) throws IOException, InterruptedException {
        runCommand(exe, exe(tools.pythonExeDir, exe), List.of(args), null, null);
    }

    private void runFF(String ff, List<String> args, Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
        List<String> list = new ArrayList<>();
        list.addAll(List.of("-v", "quiet"));
        list.addAll(args);
        runCommand(ff, exe(tools.ffmpegDir, ff), list, out, exitOk);
    }

    public void runFFMPEG(List<String> args) throws IOException, InterruptedException {
        runFF("ffmpeg", args, null, null);
    }

    public void runFFProbe(List<String> args, Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
        runFF("ffprobe", args, out, exitOk);
    }

    public String runFFProbe(List<String> args) throws IOException, InterruptedException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        runFFProbe(args, p -> {
            try {
                p.getInputStream().transferTo(bos);
            } catch (IOException ex) {
                // ignore
            }
        }, null);
        return bos.toString(StandardCharsets.UTF_8);
    }
}
