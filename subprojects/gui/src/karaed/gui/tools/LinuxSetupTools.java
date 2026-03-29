package karaed.gui.tools;

import karaed.json.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public final class LinuxSetupTools extends SetupTools {

    private Path pythonDir;
    private Path pythonExeDir;
    private Path ffmpegBinDir;

    public LinuxSetupTools(Path pythonDir, Path pythonExeDir, Path ffmpegBinDir) {
        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonExeDir;
        this.ffmpegBinDir = ffmpegBinDir;
    }

    private static Path pathsConfig() {
        return appDir().resolve("tools.json");
    }

    private record PathsConfig(
        String pythonDir,
        String pythonExeDir,
        String ffmpegBinDir
    ) {}

    private static Path getPath(PathsConfig config, Function<PathsConfig, String> field) {
        if (config != null) {
            String path = field.apply(config);
            if (path != null)
                return Path.of(path);
        }
        return Path.of("/usr/bin");
    }

    public static LinuxSetupTools create() {
        PathsConfig config = null;
        try {
            config = JsonUtil.readFile(pathsConfig(), PathsConfig.class, () -> null);
        } catch (IOException ex) {
            // ignore
        }
        Path pythonDir = getPath(config, PathsConfig::pythonDir);
        Path pythonExeDir = getPath(config, PathsConfig::pythonExeDir);
        Path ffmpegBinDir = getPath(config, PathsConfig::ffmpegBinDir);
        return new LinuxSetupTools(pythonDir, pythonExeDir, ffmpegBinDir);
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

    void setPaths(Path pythonDir, Path pythonExeDir, Path ffmpegBinDir) throws IOException {
        PathsConfig config = new PathsConfig(
            pythonDir.toAbsolutePath().toString(),
            pythonExeDir.toAbsolutePath().toString(),
            ffmpegBinDir.toAbsolutePath().toString()
        );
        Path file = pathsConfig();
        Files.createDirectories(file.getParent());
        JsonUtil.writeFile(file, config);

        this.pythonDir = pythonDir;
        this.pythonExeDir = pythonExeDir;
        this.ffmpegBinDir = ffmpegBinDir;
    }
}
