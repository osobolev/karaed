package karaed.gui.options;

import karaed.engine.opts.OInput;
import karaed.engine.video.FileStreamUtil;
import karaed.gui.tools.SetupTools;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.InputUtil;
import karaed.tools.ToolRunner;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

final class InputPanel extends BasePanel<OInput> {

    private final BaseWindow owner;
    private final SetupTools tools;
    private final JRadioButton rbURL = new JRadioButton("URL:");
    private final JRadioButton rbFile = new JRadioButton("File:");
    private final JTextField tfURL = new JTextField(40);
    private final JTextField tfFile = new JTextField(40);
    private final JButton btnBrowse = InputUtil.getChooseButtonFor(tfURL, ">", this::openURL);
    private final JButton btnChoose = InputUtil.getChooseButtonFor(tfFile, "...", () -> {
        File file = InputUtil.chooseFile(main, new FileNameExtensionFilter("MP3 files", "mp3"));
        if (file == null)
            return;
        InputUtil.setText(tfFile, file.getAbsolutePath());
    });

    InputPanel(OptCtx ctx, String defaultURL) throws IOException {
        super(ctx, null, () -> ctx.file("input.json"), OInput.class, OInput::new);
        this.owner = ctx.owner;
        this.tools = ctx.tools;

        main.add(rbURL, new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfURL, new GridBagConstraints(
            1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(btnBrowse, new GridBagConstraints(
            2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));

        main.add(rbFile, new GridBagConstraints(
            0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfFile, new GridBagConstraints(
            1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(btnChoose, new GridBagConstraints(
            2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));

        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

        btnBrowse.setToolTipText("Browse URL");
        btnChoose.setToolTipText("Choose file");

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbURL);
        bg.add(rbFile);

        if (defaultURL != null) {
            rbURL.setSelected(true);
            InputUtil.setText(tfURL, defaultURL);
        } else if (origData.file() != null) {
            rbFile.setSelected(true);
            InputUtil.setText(tfFile, origData.file());
        } else {
            rbURL.setSelected(true);
            InputUtil.setText(tfURL, origData.url());
        }

        FileDnD.allowDrop(tfFile, path -> {
            if (path.isFile()) {
                return path;
            } else {
                return null;
            }
        });

        enableDisable();
        rbURL.addActionListener(e -> enableDisable());
        rbFile.addActionListener(e -> enableDisable());
    }

    private void enableDisable() {
        tfURL.setEnabled(rbURL.isSelected());
        btnBrowse.setEnabled(rbURL.isSelected());
        tfFile.setEnabled(rbFile.isSelected());
        btnChoose.setEnabled(rbFile.isSelected());
    }

    @Override
    OInput newData() throws ValidationException {
        if (rbURL.isSelected()) {
            String url = tfURL.getText();
            if (url.trim().isEmpty()) {
                throw new ValidationException("Input URL", tfURL);
            }
            boolean valid;
            try {
                String scheme = URI.create(url).getScheme();
                valid = scheme != null;
            } catch (Exception ex) {
                valid = false;
            }
            if (!valid) {
                throw new ValidationException("Input valid URL", tfURL);
            }
            return new OInput(url, null);
        } else {
            String file = tfFile.getText();
            if (file.trim().isEmpty()) {
                throw new ValidationException("Input file path", tfFile);
            }
            Path path;
            try {
                path = Path.of(file);
            } catch (Exception ex) {
                throw new ValidationException("Invalid file name", tfFile);
            }
            if (!Files.exists(path)) {
                throw new ValidationException("File does not exist", tfFile);
            }
            boolean hasVideo;
            try {
                ToolRunner runner = ctx.runner();
                hasVideo = !FileStreamUtil.listVideoStreams(runner, path).isEmpty();
            } catch (Exception ex) {
                throw new ValidationException("File is not a video or audio file", tfFile);
            }
            return new OInput(null, file);
        }
    }

    private void openURL() {
        try {
            URI uri = URI.create(tfURL.getText());
            if (uri.getScheme() == null)
                return;
            Desktop.getDesktop().browse(uri);
        } catch (Exception ex) {
            owner.error(ex.toString());
        }
    }
}
