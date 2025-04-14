package karaed.tools;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
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
                                  Consumer<Process> out, IntPredicate exitOk) throws IOException, InterruptedException {
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
            if (out != null) {
                out.accept(p);
            } else {
                eatOutput(p.errorReader());
                eatOutput(p.inputReader());
            }
            exitCode = p.waitFor();
        } finally {
            running.remove(p);
        }
        boolean ok;
        if (exitOk != null) {
            ok = exitOk.test(exitCode);
        } else {
            ok = exitCode == 0;
        }
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

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void kill(ProcessHandle ph) {
        System.out.printf("Killing %s%n", ph.info().command().map(ProcUtil::getSimpleName).orElse("-"));
        ph.descendants().forEach(ProcessHandle::destroy);
        ph.destroy();
    }

    private static void killRunning() {
        running.forEach(p -> kill(p.toHandle()));
    }

    public static void registerShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(ProcUtil::killRunning));
    }
}
