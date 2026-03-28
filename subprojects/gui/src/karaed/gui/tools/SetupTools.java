package karaed.gui.tools;

import karaed.tools.Tools;

public abstract class SetupTools {

    public static SetupTools create() {
        return WindowsSetupTools.create();
    }

    public abstract Tools toTools();

    abstract boolean installed(Tool tool);
}
