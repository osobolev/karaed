package karaed.gui.util;

import karaed.gui.ErrorLogger;

import javax.swing.*;
import java.awt.Window;

public class BaseDialog extends JDialog implements BaseWindow {

    private final ErrorLogger logger;

    public BaseDialog(Window owner, ErrorLogger logger, String title) {
        super(owner, title, DEFAULT_MODALITY_TYPE);
        this.logger = logger;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        WindowUtil.initWindow(this, this::onClosing);
    }

    @Override
    public Window toWindow() {
        return this;
    }

    @Override
    public ErrorLogger getLogger() {
        return logger;
    }
}
