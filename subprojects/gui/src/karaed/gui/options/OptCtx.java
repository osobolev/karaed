package karaed.gui.options;

import karaed.workdir.Workdir;

import java.nio.file.Path;

final class OptCtx {

    Workdir workDir;

    OptCtx(Workdir workDir) {
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
