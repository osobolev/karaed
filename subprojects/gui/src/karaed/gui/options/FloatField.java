package karaed.gui.options;

import karaed.gui.util.InputUtil;

import javax.swing.*;

final class FloatField extends JTextField {

    FloatField() {
        super(8);
    }

    void setValue(double value) {
        InputUtil.setText(this, String.valueOf(value));
    }

    Double getValue() {
        String text = getText();
        if (text.trim().isEmpty())
            return null;
        try {
            return Double.valueOf(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    double requireValue() throws ValidationException {
        Double value = getValue();
        if (value == null)
            throw new ValidationException("Enter value", this);
        return value.doubleValue();
    }
}
