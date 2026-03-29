package karaed.gui.tools;

import karaed.gui.util.BaseDialog;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

final class LinuxPathsDialog extends BaseDialog {

    private final LinuxSetupTools tools;

    private final JTextField tfPython;
    private final JTextField tfPythonExe;
    private final JTextField tfFFmpeg;

    private boolean ok = false;

    private static JTextField newField(Path path) {
        JTextField tf = new JTextField(30);
        InputUtil.setText(tf, path.toAbsolutePath().toString());
        return tf;
    }

    LinuxPathsDialog(BaseWindow owner, LinuxSetupTools tools) {
        super(owner, "Tool paths");
        this.tools = tools;

        this.tfPython = newField(tools.pythonDir());
        this.tfPythonExe = newField(tools.pythonExeDir());
        this.tfFFmpeg = newField(tools.ffmpegBinDir());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Paths to:"));
        add(top, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        add(main, BorderLayout.CENTER);

        main.add(new JLabel("Python:"), new GridBagConstraints(
            0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfPython, new GridBagConstraints(
            1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));

        main.add(new JLabel("Python tools:"), new GridBagConstraints(
            0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfPythonExe, new GridBagConstraints(
            1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));

        main.add(new JLabel("ffmpeg:"), new GridBagConstraints(
            0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
        ));
        main.add(tfFFmpeg, new GridBagConstraints(
            1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));

        JPanel butt = new JPanel();
        butt.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                savePaths();
            }
        }));
        butt.add(new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }));
        add(butt, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void showPathError(JTextField tf, String message) {
        error(message);
        tf.requestFocusInWindow();
    }

    private Path getPath(JTextField tf) {
        String text = tf.getText();
        if (text.trim().isEmpty()) {
            showPathError(tf, "Input path");
            return null;
        }
        Path path;
        try {
            path = Path.of(text);
        } catch (InvalidPathException ex) {
            showPathError(tf, "Invalid path");
            return null;
        }
        if (!Files.exists(path)) {
            showPathError(tf, "Path does not exist");
            return null;
        }
        if (!Files.isDirectory(path)) {
            showPathError(tf, "Path is not a directory");
            return null;
        }
        return path;
    }

    private boolean checkInstalled(LinuxSetupTools newTools, JTextField tf, Tool tool) {
        if (!newTools.installed(tool)) {
            showPathError(tf, tool + " does not exist at this path");
            return false;
        }
        return true;
    }

    private void savePaths() {
        Path python = getPath(tfPython);
        if (python == null)
            return;
        Path pythonExe = getPath(tfPythonExe);
        if (pythonExe == null)
            return;
        Path ffmpeg = getPath(tfFFmpeg);
        if (ffmpeg == null)
            return;
        LinuxSetupTools newTools = new LinuxSetupTools(python, pythonExe, ffmpeg);
        if (!checkInstalled(newTools, tfPython, Tool.PYTHON))
            return;
        if (!checkInstalled(newTools, tfPythonExe, Tool.PIP))
            return;
        if (!checkInstalled(newTools, tfFFmpeg, Tool.FFMPEG))
            return;
        try {
            tools.setPaths(python, pythonExe, ffmpeg);
            ok = true;
            dispose();
        } catch (IOException ex) {
            error(ex);
        }
    }

    static void requirePaths(LinuxSetupContext ctx) throws InterruptedException {
        LinuxPathsDialog dlg = new LinuxPathsDialog(ctx.owner, ctx.lintools);
        dlg.setVisible(true);
        if (!dlg.ok) {
            throw new InterruptedException();
        }
    }
}
