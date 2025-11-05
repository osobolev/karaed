package karaed.gui.options;

import karaed.engine.opts.OAlign;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class AlignPanel extends BasePanel<OAlign> {

    private final JCheckBox cbWords = new JCheckBox("Align by words");

    AlignPanel(OptCtx ctx) throws IOException {
        super("Subs alignment", () -> ctx.option("align.json"), OAlign.class, OAlign::new);

        main.add(
            cbWords, new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        cbWords.setSelected(origData.words());
    }

    @Override
    OAlign newData() {
        return new OAlign(cbWords.isSelected());
    }
}
