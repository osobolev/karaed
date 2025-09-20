package karaed.gui.start;

import karaed.project.Workdir;

import java.nio.file.Files;
import java.nio.file.Path;

public enum DirStatus {
    OK, DOES_NOT_EXIST, NOT_A_DIRECTORY, NOT_A_PROJECT;

    public static DirStatus test(Workdir workDir) {
        Path dir = workDir.dir();
        if (!Files.exists(dir)) {
            return DOES_NOT_EXIST;
        }
        if (!Files.isDirectory(dir)) {
            return NOT_A_DIRECTORY;
        }
        Path input = workDir.file("input.json");
        if (!Files.isRegularFile(input)) {
            return NOT_A_PROJECT;
        }
        return OK;
    }

    public String getText(Workdir workDir) {
        Path dir = workDir.dir();
        return switch (this) {
            case OK -> null;
            case DOES_NOT_EXIST -> "Directory does not exist";
            case NOT_A_DIRECTORY -> dir.getFileName().toString() + " is not a directory";
            case NOT_A_PROJECT -> dir.getFileName().toString() + " is not a project directory";
        };
    }
}
