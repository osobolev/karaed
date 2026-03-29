package karaed.tools;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public interface Tools {

    Path pythonDir();

    Path pythonExeDir();

    Path ffmpegBinDir();

    private static Path exe(Path dir, String name) {
        return dir == null ? Path.of(name) : dir.resolve(name);
    }

    default Path python() {
        return exe(pythonDir(), "python");
    }

    default Path pythonTool(String tool) {
        return exe(pythonExeDir(), tool);
    }

    default Path ffmpegTool(String tool) {
        return exe(ffmpegBinDir(), tool);
    }

    default List<Path> ffmpegDirs() {
        if (ffmpegBinDir() != null) {
            return Collections.singletonList(ffmpegBinDir());
        } else {
            return Collections.emptyList();
        }
    }
}
