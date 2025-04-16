package karaed.gui.options;

import karaed.engine.opts.OCut;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class CutPanel extends BasePanel<OCut> {

    private final JTextField tfFrom = new JTextField(8);
    private final JTextField tfTo = new JTextField(8);

    CutPanel(OptCtx ctx) throws IOException {
        super("Cut", () -> ctx.option("cut.json"), OCut.class, OCut::new);

        main.add(new JLabel("From:"), new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfFrom, new GridBagConstraints(
            1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 10), 0, 0
        ));
        main.add(new JLabel("To:"), new GridBagConstraints(
            2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfTo, new GridBagConstraints(
            3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 10), 0, 0
        ));
        main.add(new JLabel("(format: 5m10s or 5:10 or simply number of seconds)"), new GridBagConstraints(
            4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        InputUtil.setText(tfFrom, origData.from());
        InputUtil.setText(tfTo, origData.to());
    }

    private static String cleanup(JTextField tf) {
        String text = tf.getText();
        String trimmed = text.trim();
        if (trimmed.isEmpty())
            return null;
        return trimmed;
    }

    private static boolean validateTime(String text) {
        if (text == null)
            return true;
        try {
            Double time = OCut.parseTime(text);
            return time != null;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    OCut newData() throws ValidationException {
        String from = cleanup(tfFrom);
        if (!validateTime(from)) {
            throw new ValidationException("Wrong From time value", tfFrom);
        }
        String to = cleanup(tfTo);
        if (!validateTime(to)) {
            throw new ValidationException("Wrong To time value", tfTo);
        }
        return new OCut(from, to);
    }
}
