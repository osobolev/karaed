package karaed.gui.options;

import karaed.gui.tools.SetupTools;
import karaed.gui.util.BaseWindow;
import karaed.project.Workdir;

import java.nio.file.Path;

final class OptCtx {

    final BaseWindow owner;
    final SetupTools tools;
    Workdir workDir;

    OptCtx(BaseWindow owner, SetupTools tools, Workdir workDir) {
        this.owner = owner;
        this.tools = tools;
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
