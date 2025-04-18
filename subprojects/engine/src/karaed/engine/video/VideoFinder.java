package karaed.engine.video;

import java.io.IOException;
import java.nio.file.Path;

public interface VideoFinder {

    Path getDir();

    String getBaseName();

    Path getVideo(String suffix, boolean required) throws IOException;
}
