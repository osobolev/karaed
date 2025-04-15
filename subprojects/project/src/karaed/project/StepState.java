package karaed.project;

public interface StepState {

    record Done() implements StepState {}

    record NotRan() implements StepState {}

    record MustRerun(String because) implements StepState {}
}
