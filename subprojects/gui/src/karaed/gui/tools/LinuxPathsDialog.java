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
import java.util.EnumMap;
import java.util.Map;

final class LinuxPathsDialog extends BaseDialog {

    private final LinuxSetupTools tools;

    private final Map<Tool, JTextField> dirFields = new EnumMap<>(Tool.class);

    private boolean ok = false;

    private static JTextField newField(Path path) {
        JTextField tf = new JTextField(30);
        InputUtil.setText(tf, path.toAbsolutePath().toString());
        return tf;
    }

    LinuxPathsDialog(BaseWindow owner, LinuxSetupTools tools) {
        super(owner, "Tool paths");
        this.tools = tools;

        dirFields.put(Tool.PYTHON, newField(tools.pythonDir()));
        dirFields.put(Tool.PIP, newField(tools.pythonExeDir()));
        dirFields.put(Tool.FFMPEG, newField(tools.ffmpegBinDir()));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Paths to:"));
        add(top, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridBagLayout());
        main.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        add(main, BorderLayout.CENTER);

        int row = 0;
        for (Map.Entry<Tool, JTextField> entry : dirFields.entrySet()) {
            Tool tool = entry.getKey();
            main.add(new JLabel(tool + ":"), new GridBagConstraints(
                0, row, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));
            main.add(entry.getValue(), new GridBagConstraints(
                1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
            ));
            row++;
        }

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
        Map<Tool, Path> dirs = new EnumMap<>(Tool.class);
        for (Map.Entry<Tool, JTextField> entry : dirFields.entrySet()) {
            Path dir = getPath(entry.getValue());
            if (dir == null)
                return;
            dirs.put(entry.getKey(), dir);
        }
        Path python = dirs.get(Tool.PYTHON);
        Path pythonExe = dirs.get(Tool.PIP);
        Path ffmpeg = dirs.get(Tool.FFMPEG);
        LinuxSetupTools newTools = new LinuxSetupTools(python, pythonExe, ffmpeg);
        for (Map.Entry<Tool, JTextField> entry : dirFields.entrySet()) {
            if (!checkInstalled(newTools, entry.getValue(), entry.getKey()))
                return;
        }
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
