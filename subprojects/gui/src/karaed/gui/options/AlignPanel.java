package karaed.gui.options;

import karaed.engine.opts.OAlign;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class AlignPanel extends BasePanel<OAlign> {

    private final JCheckBox cbWords = new JCheckBox("Align by words");
    private final FloatField tfPause = new FloatField();

    AlignPanel(OptCtx ctx) throws IOException {
        super("Subs alignment", () -> ctx.option("align.json"), OAlign.class, OAlign::new);

        main.add(
            cbWords, new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        main.add(new JLabel("Minimum seconds to tag pause:"), new GridBagConstraints(
            0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfPause, new GridBagConstraints(
            1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        cbWords.setSelected(origData.words());
        tfPause.setValue(origData.tagPause());

        cbWords.addActionListener(e -> enableDisable());
        enableDisable();
    }

    private void enableDisable() {
        tfPause.setEnabled(cbWords.isSelected());
    }

    @Override
    OAlign newData() throws ValidationException {
        double tagPause = tfPause.requireValue();
        return new OAlign(cbWords.isSelected(), tagPause);
    }
}
