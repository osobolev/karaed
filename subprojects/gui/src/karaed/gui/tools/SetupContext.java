package karaed.gui.tools;

import java.io.IOException;

abstract class SetupContext {

    final SetupTools tools;

    SetupContext(SetupTools tools) {
        this.tools = tools;
    }

    abstract String checkFFMPEGUpdate() throws IOException, InterruptedException;
}
