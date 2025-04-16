package karaed.gui.options;

import karaed.engine.opts.ODemucs;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class DemucsPanel extends BasePanel<ODemucs> {

    private final JSpinner chShifts = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));

    DemucsPanel(OptCtx ctx) throws IOException {
        super("Demucs", () -> ctx.option("demucs.json"), ODemucs.class, ODemucs::new);

        main.add(new JLabel("Number of shifts:"), new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(chShifts, new GridBagConstraints(
            1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        chShifts.setValue(origData.shifts());
    }

    @Override
    ODemucs newData() throws ValidationException {
        Number number = (Number) chShifts.getValue();
        if (number == null) {
            throw new ValidationException("Input number of shifts", chShifts);
        }
        return new ODemucs(number.intValue());
    }
}
