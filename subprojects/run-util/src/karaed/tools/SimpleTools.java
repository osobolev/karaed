package karaed.tools;

import java.nio.file.Path;

public final class SimpleTools implements Tools {

    private final Path pythonDir;
    private final Path pythonExeDir;
    private final Path ffmpegBinDir;

    public SimpleTools(Path pythonDir, Path pythonExeDir, Path ffmpegBinDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonExeDir;
        this.ffmpegBinDir = ffmpegBinDir;
    }

    @Override
    public Path pythonDir() {
        return pythonDir;
    }

    @Override
    public Path pythonExeDir() {
        return pythonExeDir;
    }

    @Override
    public Path ffmpegBinDir() {
        return ffmpegBinDir;
    }
}
