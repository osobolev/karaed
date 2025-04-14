package karaed.gui.options;

import javax.swing.*;

final class ValidationException extends Exception {

    final JComponent component;

    ValidationException(String message, JComponent component) {
        super(message);
        this.component = component;
    }
}
