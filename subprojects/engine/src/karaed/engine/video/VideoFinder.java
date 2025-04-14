package karaed.engine.video;

import karaed.engine.KaraException;
import karaed.engine.formats.info.Info;
import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Path;

public final class VideoFinder {

    private final Path dir;
    private final String base;
    private final String ext;

    private VideoFinder(Path dir, String base, String ext) {
        this.dir = dir;
        this.base = base;
        this.ext = ext;
    }

    public static String nameWithoutExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name;
        } else {
            return name.substring(0, dot);
        }
    }

    public static Path getInfoFile(Path audio, String base) {
        return audio.resolveSibling(base + ".info.json");
    }

    private static String getVideoExt(Path audio, String base) throws IOException {
        Path infoFile = getInfoFile(audio, base);
        Info info = JsonUtil.readFile(infoFile, Info.class);
        return info.ext();
    }

    public static VideoFinder maybeCreate(Path audio) throws IOException {
        String base = nameWithoutExtension(audio);
        String ext = getVideoExt(audio, base);
        if (ext == null)
            return null;
        return new VideoFinder(audio.getParent(), base, ext);
    }

    public static VideoFinder create(Path audio) throws IOException {
        VideoFinder finder = maybeCreate(audio);
        if (finder == null)
            throw new KaraException("Video file extension is not found");
        return finder;
    }

    public Path getVideoFile(String suffix) {
        return dir.resolve(base + "." + suffix + ext);
    }

    public Path getVideoFile() {
        return getVideoFile("");
    }
}
