package karaed.project;

import karaed.engine.opts.OInput;
import karaed.engine.opts.OVideo;

import java.util.*;

import static karaed.project.ProjectFile.*;

final class Dependencies {

    private final Map<ProjectFile, Set<ProjectFile>> dependencies = new EnumMap<>(ProjectFile.class);
    private final Map<PipeStep, Set<ProjectFile>> forSteps = new EnumMap<>(PipeStep.class);

    private void add(ProjectFile file, List<ProjectFile> deps) {
        dependencies.computeIfAbsent(file, k -> EnumSet.noneOf(ProjectFile.class)).addAll(deps);
    }

    private void step(PipeStep step, List<ProjectFile> files) {
        forSteps.computeIfAbsent(step, k -> EnumSet.noneOf(ProjectFile.class)).addAll(files);
    }

    Dependencies(OInput input, OVideo video) {
        if (input.url() != null) {
            add(ORIGINAL_VIDEO, List.of(INPUT));
            add(PREPARED_VIDEO, List.of(ORIGINAL_VIDEO));
            add(AUDIO, List.of(ORIGINAL_VIDEO));
        } else if (input.file() != null) {
            add(AUDIO, List.of(INPUT));
        }
        add(INFO, List.of(INPUT));
        add(VOCALS, List.of(AUDIO));
        add(NO_VOCALS, List.of(AUDIO));
        add(RANGES, List.of(TEXT, VOCALS));
        add(ALIGNED, List.of(VOCALS, RANGES));
        add(SUBS, List.of(TEXT, ALIGNED));
        add(KARAOKE_SUBS, List.of(INFO, SUBS));
        if (video.useOriginalVideo()) {
            add(KARAOKE_VIDEO, List.of(PREPARED_VIDEO, NO_VOCALS, KARAOKE_SUBS));
        } else {
            add(KARAOKE_VIDEO, List.of(NO_VOCALS, KARAOKE_SUBS));
        }

        if (input.url() != null) {
            step(PipeStep.DOWNLOAD, List.of(ORIGINAL_VIDEO, AUDIO, INFO));
        } else if (input.file() != null) {
            step(PipeStep.DOWNLOAD, List.of(AUDIO, INFO));
        }
        step(PipeStep.DEMUCS, List.of(VOCALS, NO_VOCALS));
        step(PipeStep.RANGES, List.of(RANGES));
        step(PipeStep.ALIGN, List.of(ALIGNED));
        step(PipeStep.SUBS, List.of(SUBS));
        step(PipeStep.KARAOKE, List.of(KARAOKE_SUBS));
        if (video.useOriginalVideo()) {
            step(PipeStep.PREPARE_VIDEO, List.of(PREPARED_VIDEO));
        }
        step(PipeStep.VIDEO, List.of(KARAOKE_VIDEO));
    }

    Set<ProjectFile> dependencies(ProjectFile file) {
        return dependencies.getOrDefault(file, Collections.emptySet());
    }

    Set<ProjectFile> stepFiles(PipeStep step) {
        return forSteps.getOrDefault(step, Collections.emptySet());
    }
}
