package karaed.gui.options;

import karaed.engine.opts.OKaraoke;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class KaraokePanel extends BasePanel<OKaraoke> {

    private final FloatField tfBetweenGroups = new FloatField();
    private final FloatField tfShift = new FloatField();
    private final FloatField tfPreview1 = new FloatField();
    private final FloatField tfPreview = new FloatField();
    private final FloatField tfPreviewAfterSolo = new FloatField();
    private final FloatField tfMinSoloLength = new FloatField();
    private final FloatField tfMinTitles = new FloatField();
    private final FloatField tfMaxTitles = new FloatField();
    private final FloatField tfMinAfterTitles = new FloatField();

    KaraokePanel(OptCtx ctx) throws IOException {
        super("Karaoke", () -> ctx.option("karaoke.json"), OKaraoke.class, OKaraoke::new);

        Layouter layouter = new Layouter();
        layouter.add("Seconds between verses:", tfBetweenGroups);
        layouter.add("Seconds to shift karaoke back:", tfShift);

        layouter.add("Seconds to show first line of song beforehand:", tfPreview1);
        layouter.add("Seconds to show first line of verse beforehand:", tfPreview);
        layouter.add("Seconds to show first line of verse beforehand after music solo:", tfPreviewAfterSolo);
        layouter.add("Minimum seconds for music solo to show countdown:", tfMinSoloLength);

        layouter.add("Minimum seconds to show song title:", tfMinTitles);
        layouter.add("Maximum seconds to show song title:", tfMaxTitles);
        layouter.add("Minimum seconds between title and song start:", tfMinAfterTitles);

        tfBetweenGroups.setValue(origData.betweenGroups());
        tfShift.setValue(origData.shift());

        tfPreview1.setValue(origData.preview1());
        tfPreview.setValue(origData.preview());
        tfPreviewAfterSolo.setValue(origData.previewAfterSolo());
        tfMinSoloLength.setValue(origData.minSoloLength());

        tfMinTitles.setValue(origData.minTitles());
        tfMaxTitles.setValue(origData.maxTitles());
        tfMinAfterTitles.setValue(origData.minAfterTitles());
    }

    private final class Layouter {

        int row = 0;

        void add(String label, JComponent field) {
            main.add(new JLabel(label), new GridBagConstraints(
                0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));
            main.add(field, new GridBagConstraints(
                1, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));

            row++;
        }
    }

    @Override
    OKaraoke newData() throws ValidationException {
        double betweenGroups = tfBetweenGroups.requireValue();
        double shift = tfShift.requireValue();

        double preview1 = tfPreview1.requireValue();
        double preview = tfPreview.requireValue();
        double previewAfterSolo = tfPreviewAfterSolo.requireValue();
        double minSoloLength = tfMinSoloLength.requireValue();

        double minTitles = tfMinTitles.requireValue();
        double maxTitles = tfMaxTitles.requireValue();
        double minAfterTitles = tfMinAfterTitles.requireValue();

        return new OKaraoke(
            betweenGroups, shift,
            preview1, preview, previewAfterSolo, minSoloLength,
            minTitles, maxTitles, minAfterTitles
        );
    }
}
