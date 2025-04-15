package karaed.project;

import java.nio.file.Path;
import java.util.List;

interface FileState {

    record Fresh() implements FileState {}

    record Missing() implements FileState {}

    record MustRebuild(List<Path> newer, List<Path> rebuilt) implements FileState {}
}
