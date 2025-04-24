package karaed.project;

import java.util.Map;

public record PipeInfo(
    Map<PipeStep, StepState> stepStates,
    boolean hasVideo
) {}
