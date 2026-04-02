package karaed.gui.options;

import karaed.engine.opts.OAlign;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class AlignPanel extends BasePanel<OAlign> {

    private final JCheckBox cbWords = new JCheckBox("Align by words");
    private final JRadioButton rbVocals = new JRadioButton("Vocals only");
    private final JRadioButton rbFull = new JRadioButton("Full audio");

    AlignPanel(OptCtx ctx) throws IOException {
        super(ctx, "Subtitles", () -> ctx.option("align.json"), OAlign.class, OAlign::new);

        ButtonGroup group = new ButtonGroup();
        group.add(rbVocals);
        group.add(rbFull);

        main.add(cbWords, new GridBagConstraints(
            1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(new JLabel("Associate subtitles with:"), new GridBagConstraints(
            0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(rbVocals, new GridBagConstraints(
            1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(rbFull, new GridBagConstraints(
            2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        cbWords.setSelected(origData.words());
        if (origData.vocalsOnly()) {
            rbVocals.setSelected(true);
        } else {
            rbFull.setSelected(true);
        }
    }

    @Override
    OAlign newData() {
        return new OAlign(cbWords.isSelected(), rbVocals.isSelected());
    }
}
