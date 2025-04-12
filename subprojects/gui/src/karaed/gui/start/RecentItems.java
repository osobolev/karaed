package karaed.gui.start;

import karaed.gui.ErrorLogger;
import karaed.json.JsonUtil;
import karaed.workdir.Workdir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class RecentItems {

    public static String isProjectDir(Workdir workDir) {
        Path dir = workDir.dir();
        if (!Files.exists(dir)) {
            return "Directory does not exist";
        }
        if (!Files.isDirectory(dir)) {
            return dir.getFileName().toString() + " is not a directory";
        }
        Path input = workDir.file("input.json");
        if (!Files.isRegularFile(input)) {
            return dir.getFileName().toString() + " is not a project directory";
        }
        return null;
    }

    private static Path getRecentFile() {
        String homeDir = System.getProperty("user.home");
        if (homeDir == null)
            return null;
        return Path.of(homeDir, ".karaed", "recent.json");
    }

    private static RecentModel loadModel(ErrorLogger logger, Path recentFile) {
        try {
            return JsonUtil.readFile(recentFile, RecentModel.class, () -> EMPTY);
        } catch (Exception ex) {
            logger.error(ex);
            return EMPTY;
        }
    }

    private static void saveModel(ErrorLogger logger, Path recentFile, RecentModel model) {
        try {
            Files.createDirectories(recentFile.getParent());
            JsonUtil.writeFile(recentFile, model);
        } catch (Exception ex) {
            logger.error(ex);
        }
    }

    public static List<Path> loadRecentItems(ErrorLogger logger) {
        Path recentFile = getRecentFile();
        if (recentFile == null)
            return Collections.emptyList();
        RecentModel model = loadModel(logger, recentFile);
        List<Path> dirs = new ArrayList<>();
        for (RecentDir dir : model.recent()) {
            dirs.add(Path.of(dir.dir()));
        }
        return dirs;
    }

    public static void addRecentItem(ErrorLogger logger, Path dir) {
        Path recentFile = getRecentFile();
        if (recentFile == null)
            return;
        RecentModel model = loadModel(logger, recentFile);
        List<RecentDir> newRecent = new ArrayList<>();
        newRecent.add(new RecentDir(dir.toAbsolutePath().toString()));
        Set<Path> visited = new HashSet<>();
        visited.add(dir.normalize());
        for (RecentDir recentDir : model.recent()) {
            Path idir = Path.of(recentDir.dir()).normalize();
            if (!visited.add(idir))
                continue;
            newRecent.add(recentDir);
        }
        saveModel(logger, recentFile, new RecentModel(newRecent));
    }

    public static void removeRecentItem(ErrorLogger logger, Path dir) {
        Path recentFile = getRecentFile();
        if (recentFile == null)
            return;
        RecentModel model = loadModel(logger, recentFile);
        List<RecentDir> newRecent = new ArrayList<>();
        Path ndir = dir.normalize();
        for (RecentDir recentDir : model.recent()) {
            Path idir = Path.of(recentDir.dir()).normalize();
            if (Objects.equals(idir, ndir))
                continue;
            newRecent.add(recentDir);
        }
        saveModel(logger, recentFile, new RecentModel(newRecent));
    }

    private static final RecentModel EMPTY = new RecentModel(Collections.emptyList());

    private record RecentModel(
        List<RecentDir> recent
    ) {}

    private record RecentDir(
        String dir
    ) {}
}
