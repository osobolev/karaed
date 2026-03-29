package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;

abstract class SetupContext {

    final SetupTools tools;

    SetupContext(SetupTools tools) {
        this.tools = tools;
    }

    abstract String checkFFMPEGUpdate() throws IOException, InterruptedException;

    abstract void updateFFMPEG(ToolRunner runner) throws IOException, InterruptedException;

    abstract InstallRunner installRunner(ToolRunner runner);
}
