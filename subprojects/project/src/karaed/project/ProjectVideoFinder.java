package karaed.project;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.engine.video.VideoFinder;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Path;

final class ProjectVideoFinder implements VideoFinder {

    private final Workdir workDir;

    private String ext = null;
    private boolean extInited = false;

    ProjectVideoFinder(Workdir workDir) {
        this.workDir = workDir;
    }

    @Override
    public Path getDir() {
        return workDir.dir();
    }

    @Override
    public String getBaseName() {
        return "audio";
    }

    private String getExtension() throws IOException {
        if (!extInited) {
            Info info = JsonUtil.readFile(workDir.info(), Info.class, () -> null);
            if (info != null) {
                ext = info.ext();
                extInited = true;
            }
        }
        return ext;
    }

    @Override
    public Path getVideo(String suffix, boolean required) throws IOException {
        String extension = getExtension();
        if (extension == null) {
            if (required) {
                throw new KaraException("Video file extension is not found");
            } else {
                return null;
            }
        }
        return getDir().resolve(getBaseName() + "." + suffix + extension);
    }
}
