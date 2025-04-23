package karaed.gui.util;

import karaed.gui.ErrorLogger;

import javax.swing.*;
import java.awt.Window;

public class BaseFrame extends JFrame implements BaseWindow {

    private final ErrorLogger logger;

    public BaseFrame(ErrorLogger logger, String title) {
        super(title);
        this.logger = logger;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        CloseUtil.listen(this, this::onClosing);
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
