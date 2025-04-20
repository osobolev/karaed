package karaed.project;

import karaed.engine.opts.OInput;
import karaed.engine.opts.OVideo;
import karaed.engine.steps.video.MakeVideo;
import karaed.engine.video.VideoFinder;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.*;

public final class PipeBuilder {

    private static final class FileStateBuf {

        Path file;
        FileState state;
    }

    private final Workdir workDir;
    private final Map<ProjectFile, FileStateBuf> fileStates = new EnumMap<>(ProjectFile.class);
    private final Dependencies dependencies;

    private PipeBuilder(Workdir workDir, Dependencies dependencies) {
        this.workDir = workDir;
        this.dependencies = dependencies;
        for (ProjectFile file : ProjectFile.values()) {
            fileStates.put(file, new FileStateBuf());
        }
    }

    public static PipeBuilder create(Workdir workDir) throws IOException {
        OInput input = JsonUtil.readFile(workDir.file("input.json"), OInput.class);
        OVideo video = JsonUtil.readFile(workDir.option("video.json"), OVideo.class, OVideo::new);
        return new PipeBuilder(workDir, new Dependencies(input, video));
    }

    private void setFile(ProjectFile file, Path path) {
        fileStates.get(file).file = path;
    }

    private void fillFiles() throws IOException {
        VideoFinder finder = workDir.video();

        setFile(ProjectFile.INPUT, workDir.file("input.json"));
        setFile(ProjectFile.TEXT, workDir.file("text.txt"));
        setFile(ProjectFile.AUDIO, workDir.audio());
        setFile(ProjectFile.INFO, workDir.info());
        setFile(ProjectFile.ORIGINAL_VIDEO, finder.getVideo("", false));
        setFile(ProjectFile.PREPARED_VIDEO, finder.getVideo(MakeVideo.PREPARED, false));
        setFile(ProjectFile.VOCALS, workDir.vocals());
        setFile(ProjectFile.NO_VOCALS, workDir.noVocals());
        setFile(ProjectFile.RANGES, workDir.file("ranges.json"));
        setFile(ProjectFile.ALIGNED, workDir.file("aligned.json"));
        setFile(ProjectFile.SUBS, workDir.file("subs.ass"));
        setFile(ProjectFile.KARAOKE_SUBS, workDir.file("karaoke.ass"));
        setFile(ProjectFile.KARAOKE_VIDEO, workDir.file("karaoke.mp4"));
    }

    private String pathString(Path depFile) {
        Path dir = workDir.dir();
        if (depFile.startsWith(dir)) {
            return dir.relativize(depFile).toString();
        } else {
            return depFile.getFileName().toString();
        }
    }

    private String depName(ProjectFile dep) {
        Path depFile = fileStates.get(dep).file;
        if (depFile == null)
            return dep.toString();
        Path dir = workDir.dir();
        if (depFile.startsWith(dir)) {
            return dir.relativize(depFile).toString();
        } else {
            return depFile.getFileName().toString();
        }
    }

    private FileState determineFileState(ProjectFile file, FileStateBuf state) throws IOException {
        if (state.file == null || !Files.exists(state.file))
            return new FileState.Missing();
        FileTime myTime = Files.getLastModifiedTime(state.file);
        List<String> newer = new ArrayList<>();
        List<String> rebuilt = new ArrayList<>();
        for (Map.Entry<ProjectFile, Boolean> entry : dependencies.dependencies(file).entrySet()) {
            ProjectFile dep = entry.getKey();
            boolean required = entry.getValue().booleanValue();
            Path depFile = fileStates.get(dep).file;
            FileState depState = getFileState(dep);
            if (depState instanceof FileState.Fresh) {
                FileTime depTime = Files.getLastModifiedTime(depFile);
                if (depTime.compareTo(myTime) > 0) {
                    newer.add(depName(dep));
                }
            } else if (depState instanceof FileState.MustRebuild) {
                rebuilt.add(depName(dep));
            } else if (depState instanceof FileState.Missing && required) {
                rebuilt.add(depName(dep));
            }
        }
        for (String option : file.options) {
            Path optionFile = workDir.option(option);
            if (Files.exists(optionFile)) {
                FileTime optTime = Files.getLastModifiedTime(optionFile);
                if (optTime.compareTo(myTime) > 0) {
                    newer.add(pathString(optionFile));
                }
            }
        }
        String because;
        if (!newer.isEmpty()) {
            because = String.format(
                "%s is newer than %s",
                String.join(", ", newer), depName(file)
            );
        } else if (!rebuilt.isEmpty()) {
            because = String.format(
                "%s must be rebuilt before %s",
                String.join(", ", rebuilt), depName(file)
            );
        } else {
            return new FileState.Fresh();
        }
        return new FileState.MustRebuild(because);
    }

    private FileState getFileState(ProjectFile file) throws IOException {
        FileStateBuf state = fileStates.get(file);
        if (state.state == null) {
            state.state = determineFileState(file, state);
        }
        return state.state;
    }

    private StepState getStepState(PipeStep step) throws IOException {
        Set<ProjectFile> files = dependencies.stepFiles(step);
        for (ProjectFile file : files) {
            FileState state = getFileState(file);
            if (state instanceof FileState.Missing) {
                return new StepState.NotRan();
            }
        }
        for (ProjectFile file : files) {
            FileState state = getFileState(file);
            if (state instanceof FileState.MustRebuild(String because)) {
                return new StepState.MustRerun(because);
            }
        }
        return new StepState.Done();
    }

    public Map<PipeStep, StepState> buildPipe() throws IOException {
        fillFiles();
        for (ProjectFile file : fileStates.keySet()) {
            getFileState(file);
        }
        Map<PipeStep, StepState> stepStates = new EnumMap<>(PipeStep.class);
        for (PipeStep step : PipeStep.values()) {
            StepState stepState = getStepState(step);
            stepStates.put(step, stepState);
        }
        return stepStates;
    }
}
