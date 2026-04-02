package karaed.gui.options;

import karaed.engine.formats.info.Info;
import karaed.engine.opts.OKaraoke;
import karaed.engine.steps.youtube.Youtube;
import karaed.gui.util.InputUtil;
import karaed.gui.util.TitleUtil;

import javax.swing.*;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;

final class KaraokePanel extends BasePanel<OKaraoke> {

    private final InputPanel input;

    private final FloatField tfBetweenGroups = new FloatField();
    private final FloatField tfShift = new FloatField();
    private final FloatField tfPreview1 = new FloatField();
    private final FloatField tfPreview = new FloatField();
    private final FloatField tfPreviewAfterSolo = new FloatField();
    private final FloatField tfMinSoloLength = new FloatField();
    private final FloatField tfMinTitles = new FloatField();
    private final FloatField tfMaxTitles = new FloatField();
    private final FloatField tfMinAfterTitles = new FloatField();

    private final JCheckBox cbCustomTitle = new JCheckBox("Custom title:");
    private final JTextArea taTitle = new JTextArea(3, 20);

    KaraokePanel(OptCtx ctx, InputPanel input) throws IOException {
        super(ctx, "Karaoke", () -> ctx.option("karaoke.json"), OKaraoke.class, OKaraoke::new);
        this.input = input;

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

        layouter.addTitles();

        tfBetweenGroups.setValue(origData.betweenGroups());
        tfShift.setValue(origData.shift());

        tfPreview1.setValue(origData.preview1());
        tfPreview.setValue(origData.preview());
        tfPreviewAfterSolo.setValue(origData.previewAfterSolo());
        tfMinSoloLength.setValue(origData.minSoloLength());

        tfMinTitles.setValue(origData.minTitles());
        tfMaxTitles.setValue(origData.maxTitles());
        tfMinAfterTitles.setValue(origData.minAfterTitles());

        if (origData.title() != null) {
            cbCustomTitle.setSelected(true);
            InputUtil.setText(taTitle, origData.title());
        } else {
            cbCustomTitle.setSelected(false);
            if (ctx.workDir != null) {
                Info info = TitleUtil.getInfo(ctx.workDir);
                setTitles(info);
            }
        }

        cbCustomTitle.addActionListener(e -> enableDisable());
        enableDisable();
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

        void addTitles() {
            main.add(cbCustomTitle, new GridBagConstraints(
                0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));
            row++;

            main.add(new JScrollPane(taTitle), new GridBagConstraints(
                0, row, 2, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
            ));
            row++;
        }
    }

    private static Color getUIColor(String key) {
        Color bg = UIManager.getColor(key);
        if (bg == null)
            return null;
        return new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha());
    }

    private void enableDisable() {
        boolean editable = cbCustomTitle.isSelected();
        if (editable && taTitle.getText().isEmpty()) {
            tryLoadTitles();
        }
        taTitle.setEditable(editable);
        taTitle.setBackground(getUIColor(editable ? "TextField.background" : "TextField.inactiveBackground"));
    }

    private void tryLoadTitles() {
        new InputDetailsFetcher<Info>(ctx, input).fetch(
            true,
            Youtube::metaInfo, this::setTitles,
            ex -> false
        );
    }

    private void setTitles(Info info) {
        InputUtil.setText(taTitle, info == null ? "" : String.join("\n", info.getTitles()));
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

        String title;
        if (cbCustomTitle.isSelected()) {
            title = taTitle.getText();
        } else {
            title = null;
        }

        return new OKaraoke(
            betweenGroups, shift,
            preview1, preview, previewAfterSolo, minSoloLength,
            minTitles, maxTitles, minAfterTitles,
            title
        );
    }
}
