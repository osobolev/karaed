package karaed.gui.util;

import karaed.gui.ErrorLogger;

import java.awt.Window;

public interface BaseWindow {

    Window toWindow();

    ErrorLogger getLogger();

    default void error(Throwable ex) {
        ShowMessage.error(getLogger(), toWindow(), ex);
    }

    default void error(String message) {
        ShowMessage.error(toWindow(), message);
    }

    default boolean confirm2(String message) {
        return ShowMessage.confirm2(toWindow(), message);
    }

    default boolean onClosing() {
        return true;
    }
}
