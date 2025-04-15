package karaed.gui.project;

import karaed.project.StepState;

interface RunStepState {

    record Done() implements RunStepState {}

    record Running() implements RunStepState {}

    record Error(String message) implements RunStepState {}

    record NotRan() implements RunStepState {};

    record MustRerun(String because) implements RunStepState {};

    static RunStepState initState(StepState state) {
        if (state instanceof StepState.Done)
            return new Done();
        if (state instanceof StepState.NotRan)
            return new NotRan();
        if (state instanceof StepState.MustRerun mr)
            return new MustRerun(mr.because());
        throw new IllegalArgumentException("Unknown step state: " + state);
    }
}
