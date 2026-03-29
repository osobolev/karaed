package karaed.gui.options;

import karaed.gui.util.BaseWindow;
import karaed.project.Workdir;

import java.nio.file.Path;

final class OptCtx {

    final BaseWindow owner;
    Workdir workDir;

    OptCtx(BaseWindow owner, Workdir workDir) {
        this.owner = owner;
        this.workDir = workDir;
    }

    Path file(String name) {
        if (workDir == null)
            return null;
        return workDir.file(name);
    }

    Path option(String name) {
        if (workDir == null)
            return null;
        return workDir.option(name);
    }
}
