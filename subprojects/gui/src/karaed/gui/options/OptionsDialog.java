package karaed.gui.options;

import karaed.gui.ErrorLogger;
import karaed.gui.util.BaseDialog;
import karaed.gui.util.InputUtil;
import karaed.project.Workdir;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class OptionsDialog extends BaseDialog {

    private final JTextField tfDir;
    private final JPanel main = new JPanel();
    private final JPanel options = new JPanel();
    private final JPanel advanced = new JPanel();
    private final List<BasePanel<?>> panels = new ArrayList<>();

    private final OptCtx ctx;

    private boolean saved = false;

    private void add(BasePanel<?> panel, JPanel to) {
        to.add(panel.getVisual());
        panels.add(panel);
    }

    private OptionsDialog(ErrorLogger logger, String title, Window owner, Workdir workDir,
                          Path defaultDir, String defaultURL) throws IOException {
        super(owner, logger, title);
        this.ctx = new OptCtx(workDir);

        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        advanced.setLayout(new BoxLayout(advanced, BoxLayout.Y_AXIS));

        if (ctx.workDir == null) {
            tfDir = new JTextField(40);
            if (defaultDir != null) {
                InputUtil.setText(tfDir, defaultDir.toAbsolutePath().normalize().toString());
            }
            JButton btnChoose = InputUtil.getChooseButtonFor(tfDir, "...", () -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int ans = chooser.showSaveDialog(this);
                if (ans == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    if (file != null) {
                        tfDir.setText(file.getAbsolutePath());
                    }
                }
            });
            btnChoose.setToolTipText("Choose directory");
            JPanel top = new JPanel(new GridBagLayout());
            top.add(new JLabel("Directory:"), new GridBagConstraints(
                0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));
            top.add(tfDir, new GridBagConstraints(
                1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
            ));
            top.add(btnChoose, new GridBagConstraints(
                2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));
            top.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
            add(top, BorderLayout.NORTH);
        } else {
            tfDir = null;
        }

        add(new InputPanel(ctx, defaultURL), main);
        add(new LyricsPanel(ctx), main);
        add(new CutPanel(ctx), options);
        add(new AlignPanel(ctx), options);
        add(new VideoPanel(ctx), options);
        add(new DemucsPanel(ctx), advanced);
        add(new KaraokePanel(ctx), advanced);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Input", main);
        tabs.addTab("Options", options);
        tabs.addTab("Advanced options", advanced);

        add(tabs, BorderLayout.CENTER);

        JPanel butt = new JPanel();
        butt.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
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
        setVisible(true);
    }

    public static OptionsDialog newProject(ErrorLogger logger, Window owner, Path defaultDir, String defaultURL) throws IOException {
        return new OptionsDialog(logger, "New project", owner, null, defaultDir, defaultURL);
    }

    public static OptionsDialog options(ErrorLogger logger, Window owner, Workdir workdir) throws IOException {
        return new OptionsDialog(logger, "Options", owner, workdir, null, null);
    }

    private void save() {
        if (ctx.workDir == null) {
            String dirStr = tfDir.getText();
            if (dirStr.isEmpty()) {
                tfDir.requestFocusInWindow();
                error("Enter a directory");
                return;
            }
            Path dir = Path.of(dirStr);
            try {
                Files.createDirectories(dir);
            } catch (Exception ex) {
                error("Cannot create directory");
                return;
            }
            ctx.workDir = new Workdir(dir);
        }
        List<BasePanel.Saver> savers = new ArrayList<>();
        try {
            for (BasePanel<?> panel : panels) {
                BasePanel.Saver saver = panel.prepareToSave();
                savers.add(saver);
            }
        } catch (ValidationException ex) {
            openTabContaining(ex.component);
            ex.component.requestFocusInWindow();
            error(ex.getMessage());
            return;
        }
        saved = true;
        try {
            for (BasePanel.Saver saver : savers) {
                saver.save();
            }
            dispose();
        } catch (Exception ex) {
            error(ex);
        }
    }

    private static void openTabContaining(JComponent comp) {
        if (comp.isShowing())
            return;
        Component current = comp;
        while (current != null) {
            if (current instanceof JTabbedPane tabs) {
                for (int i = 0; i < tabs.getTabCount(); i++) {
                    Component tab = tabs.getComponentAt(i);
                    if (SwingUtilities.isDescendingFrom(comp, tab)) {
                        tabs.setSelectedIndex(i);
                        break;
                    }
                }
            }
            current = current.getParent();
        }
    }

    public Workdir getWorkDir() {
        return ctx.workDir;
    }

    public boolean isSaved() {
        return saved;
    }
}
