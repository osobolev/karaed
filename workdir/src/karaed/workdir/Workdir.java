package karaed.workdir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Workdir {

    private static final String BASE_NAME = "audio";

    private final Path workDir;

    public Workdir(Path workDir) {
        this.workDir = workDir;
    }

    private interface NameParts {

        String process(String baseName, String ext);
    }

    private static String splitName(Path file, NameParts parts) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return parts.process(name, "");
        } else {
            return parts.process(name.substring(0, dot), name.substring(dot));
        }
    }

    private static String nameWithoutExtension(Path file) {
        return splitName(file, (baseName, ext) -> baseName);
    }

    public Path file(String name) {
        return workDir.resolve(name);
    }

    public Path audio() {
        return file(BASE_NAME + ".mp3");
    }

    private Path oneOf(Supplier<Stream<String>> getNames) {
        Iterator<String> it = getNames.get().iterator();
        Path first = null;
        while (it.hasNext()) {
            String name = it.next();
            Path path = file(name);
            if (Files.exists(path))
                return path;
            if (first == null) {
                first = path;
            }
        }
        return first;
    }

    public Path video() {
        return oneOf(() -> Stream.of(".webm", ".mp4", ".webm.mp4").map(ext -> BASE_NAME + ext));
    }

    public Path info() {
        return oneOf(() -> Stream.of(BASE_NAME + ".mp3.info.json", BASE_NAME + ".info.json", "info.json"));
    }
}
