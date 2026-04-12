package karaed.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public final class TouchUtil {

    public static void touchIfSourceNewer(Path target, Path... sources) throws IOException {
        if (!Files.exists(target))
            return;
        FileTime targetTime = Files.getLastModifiedTime(target);
        FileTime maxSourceTime = null;
        for (Path source : sources) {
            FileTime sourceTime = Files.getLastModifiedTime(source);
            if (maxSourceTime == null || sourceTime.compareTo(maxSourceTime) > 0) {
                maxSourceTime = sourceTime;
            }
        }
        if (maxSourceTime != null && maxSourceTime.compareTo(targetTime) > 0) {
            Files.setLastModifiedTime(target, maxSourceTime);
        }
    }
}
