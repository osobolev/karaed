package karaed.gui.util;

import karaed.engine.formats.info.Info;
import karaed.json.JsonUtil;
import karaed.workdir.Workdir;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TitleUtil {

    public static Info getInfo(Workdir workDir) {
        try {
            Path infoFile = workDir.info();
            if (Files.exists(infoFile)) {
                return JsonUtil.readFile(infoFile, Info.class);
            }
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }
}
