package karaed.workdir;

import java.nio.file.Path;

public final class Workdir {

    private static final String BASE_NAME = "audio";

    private final Path workDir;

    public Workdir(Path workDir) {
        this.workDir = workDir;
    }

    public Path dir() {
        return workDir;
    }

    public Path file(String name) {
        return workDir.resolve(name);
    }

    public Path option(String name) {
        return file(".options/" + name);
    }

    public Path audio() {
        return file(BASE_NAME + ".mp3");
    }

    public Path demuxed(String name) {
        return file("htdemucs/" + BASE_NAME + "/" + name);
    }

    public Path vocals() {
        return demuxed("vocals.wav");
    }

    public Path info() {
        return file(BASE_NAME + ".info.json");
    }
}
