package karaed.project;

import karaed.engine.opts.OInput;
import karaed.engine.opts.OVideo;

import java.util.*;

import static karaed.project.ProjectFile.*;

final class Dependencies {

    final boolean hasVideo;
    private final Map<ProjectFile, Map<ProjectFile, Boolean>> dependencies = new EnumMap<>(ProjectFile.class);
    private final Map<PipeStep, Set<ProjectFile>> forSteps = new EnumMap<>(PipeStep.class);

    private void add(ProjectFile file, ProjectFile dep, boolean required) {
        dependencies.computeIfAbsent(file, k -> new EnumMap<>(ProjectFile.class)).put(dep, required);
    }

    private void add(ProjectFile file, ProjectFile dep) {
        add(file, dep, true);
    }

    private void fileDeps(OVideo video) {
        if (hasVideo) {
            add(ORIGINAL_VIDEO, INPUT);
            add(PREPARED_VIDEO, ORIGINAL_VIDEO);
            add(AUDIO, ORIGINAL_VIDEO);
        } else {
            add(AUDIO, INPUT);
        }

        add(INFO, INPUT);

        add(VOCALS, AUDIO);
        add(NO_VOCALS, AUDIO);

        add(RANGES, TEXT);
        add(RANGES, VOCALS);

        add(ALIGNED, VOCALS);
        add(ALIGNED, RANGES);
        add(ALIGNED, LANGUAGE, false);

        add(SUBS, TEXT);
        add(SUBS, ALIGNED);

        add(KARAOKE_SUBS, INFO, false);
        add(KARAOKE_SUBS, SUBS);

        if (hasVideo && video.useOriginalVideo()) {
            add(KARAOKE_VIDEO, PREPARED_VIDEO);
        }
        add(KARAOKE_VIDEO, NO_VOCALS);
        add(KARAOKE_VIDEO, KARAOKE_SUBS);
    }

    private void step(PipeStep step, ProjectFile file) {
        forSteps.computeIfAbsent(step, k -> EnumSet.noneOf(ProjectFile.class)).add(file);
    }

    private void stepDeps(OVideo video) {
        if (hasVideo) {
            step(PipeStep.DOWNLOAD, ORIGINAL_VIDEO);
        }
        step(PipeStep.DOWNLOAD, AUDIO);
        step(PipeStep.DOWNLOAD, INFO);

        step(PipeStep.DEMUCS, VOCALS);
        step(PipeStep.DEMUCS, NO_VOCALS);

        step(PipeStep.RANGES, RANGES);

        step(PipeStep.ALIGN, ALIGNED);
        step(PipeStep.ALIGN, LANGUAGE);

        step(PipeStep.SUBS, SUBS);

        step(PipeStep.KARAOKE, KARAOKE_SUBS);

        if (hasVideo && video.useOriginalVideo()) {
            step(PipeStep.PREPARE_VIDEO, PREPARED_VIDEO);
        }

        step(PipeStep.VIDEO, KARAOKE_VIDEO);
    }

    Dependencies(OInput input, OVideo video) {
        this.hasVideo = input.url() != null;

        fileDeps(video);

        stepDeps(video);
    }

    Map<ProjectFile, Boolean> dependencies(ProjectFile file) {
        return dependencies.getOrDefault(file, Collections.emptyMap());
    }

    Set<ProjectFile> stepFiles(PipeStep step) {
        return forSteps.getOrDefault(step, Collections.emptySet());
    }
}
