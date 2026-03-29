package karaed.gui.tools;

import karaed.gui.util.BaseWindow;
import karaed.tools.ToolRunner;

final class LinuxSetupContext extends SetupContext {

    final LinuxSetupTools lintools;
    final BaseWindow owner;

    LinuxSetupContext(LinuxSetupTools tools, BaseWindow owner) {
        super(tools);
        this.lintools = tools;
        this.owner = owner;
    }

    @Override
    String checkFFMPEGUpdate() {
        return null;
    }

    @Override
    void updateFFMPEG(ToolRunner runner) {
    }

    @Override
    InstallRunner installRunner(ToolRunner runner) {
        return new LinuxInstallRunner(this, runner);
    }
}
