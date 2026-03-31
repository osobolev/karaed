package karaed.gui.util;

import karaed.gui.ErrorLogger;

import javax.swing.*;
import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Window;

public class BaseFrame extends JFrame implements BaseWindow {

    private final ErrorLogger logger;

    private Window owner;
    private SecondaryLoop modalLoop;

    public BaseFrame(ErrorLogger logger, String title) {
        super(title);
        this.logger = logger;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        WindowUtil.initWindow(this, this::onClosing);
    }

    public void showModal(Window owner) {
        this.owner = owner;
        if (owner != null) {
            owner.setVisible(false);
            if (modalLoop == null) {
                EventQueue eq = getToolkit().getSystemEventQueue();
                modalLoop = eq.createSecondaryLoop();
                setVisible(true);
                modalLoop.enter();
            }
        } else {
            setVisible(true);
        }
    }

    @Override
    public void dispose() {
        if (modalLoop != null) {
            modalLoop.exit();
            modalLoop = null;
        }
        if (owner != null) {
            owner.setVisible(true);
        }
        super.dispose();
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
