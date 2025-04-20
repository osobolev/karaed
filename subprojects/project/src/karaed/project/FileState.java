package karaed.project;

interface FileState {

    record Fresh() implements FileState {}

    record Missing() implements FileState {}

    record MustRebuild(String because) implements FileState {}
}
