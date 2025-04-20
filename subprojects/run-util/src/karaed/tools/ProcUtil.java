package karaed.tools;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ProcUtil {

    private static final Set<Process> running = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void eatOutput(Reader rdr) {
        Thread thread = new Thread(() -> {
            try {
                rdr.transferTo(Writer.nullWriter());
            } catch (IOException ex) {
                // ignore
            }
        });
        thread.start();
    }

    public static void runCommand(String what, Path exe, List<String> args, List<Path> pathDirs,
                                  Consumer<Reader> stdout, Consumer<Reader> stderr, Consumer<String> log) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(exe.toString());
        command.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        if (!pathDirs.isEmpty()) {
            String pathEnv = "PATH";
            Set<String> pathLike = new HashSet<>();
            for (String name : pb.environment().keySet()) {
                if (pathEnv.equalsIgnoreCase(name)) {
                    pathLike.add(name);
                }
            }
            if (pathLike.isEmpty()) {
                pathLike.add(pathEnv);
            }
            String addPath = pathDirs.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
            for (String name : pathLike) {
                pb.environment().compute(name, (k, pathValue) -> pathValue == null ? addPath : addPath + File.pathSeparator + pathValue);
            }
        }
        Process p = pb.start();
        running.add(p);
        int exitCode;
        try {
            stderr.accept(p.errorReader());
            stdout.accept(p.inputReader());
            exitCode = p.waitFor();
        } catch (InterruptedException ex) {
            kill(p, log);
            throw ex;
        } finally {
            running.remove(p);
        }
        boolean ok = exitCode == 0;
        if (!ok)
            throw new IOException("Error running " + what + ": " + exitCode);
    }

    private static String getSimpleName(String command) {
        try {
            Path path = Path.of(command);
            return path.getFileName().toString();
        } catch (Exception ex) {
            // ignore
        }
        return command;
    }

    private static void kill(Process p, Consumer<String> log) {
        ProcessHandle ph = p.toHandle();
        String procName = ph.info().command().map(ProcUtil::getSimpleName).orElse("-");
        log.accept(String.format("Killing %s", procName));
        ph.descendants().forEach(ProcessHandle::destroy);
        ph.destroy();
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void killRunning() {
        running.forEach(p -> kill(p, System.out::println));
    }

    public static void registerShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(ProcUtil::killRunning));
    }
}
