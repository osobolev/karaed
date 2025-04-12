package karaed.gui.options;

import karaed.engine.opts.OInput;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

final class InputPanel extends BasePanel<OInput> {

    private final JRadioButton rbURL = new JRadioButton("URL:");
    private final JRadioButton rbFile = new JRadioButton("File:");
    private final JTextField tfURL = new JTextField(40);
    private final JTextField tfFile = new JTextField(40);
    private final JButton btnChoose = InputUtil.getChooseButtonFor(tfFile, "...", () -> {
        File file = InputUtil.chooseFile(main, new FileNameExtensionFilter("MP3 files", "mp3"));
        if (file == null)
            return;
        InputUtil.setText(tfFile, file.getAbsolutePath());
    });

    InputPanel(OptCtx ctx) throws IOException {
        super(null, () -> ctx.file("input.json"), OInput.class, OInput::new);

        main.add(rbURL, new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfURL, new GridBagConstraints(
            1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));

        main.add(rbFile, new GridBagConstraints(
            0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfFile, new GridBagConstraints(
            1, 1, 1, 1, 0.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(btnChoose, new GridBagConstraints(
            2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbURL);
        bg.add(rbFile);

        if (origData.file() != null) {
            rbFile.setSelected(true);
            InputUtil.setText(tfFile, origData.file());
        } else {
            rbURL.setSelected(true);
            InputUtil.setText(tfURL, origData.url());
        }

        enableDisable();
        rbURL.addActionListener(e -> enableDisable());
        rbFile.addActionListener(e -> enableDisable());
    }

    private void enableDisable() {
        tfURL.setEnabled(rbURL.isSelected());
        tfFile.setEnabled(rbFile.isSelected());
        btnChoose.setEnabled(rbFile.isSelected());
    }

    @Override
    OInput newData() {
        // todo: check input for validity
        if (rbURL.isSelected()) {
            return new OInput(tfURL.getText(), null);
        } else {
            return new OInput(null, tfFile.getText());
        }
    }
}
