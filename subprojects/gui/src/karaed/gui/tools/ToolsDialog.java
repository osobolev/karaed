package karaed.gui.tools;

import karaed.gui.ErrorLogger;
import karaed.gui.util.BaseDialog;
import karaed.tools.Tools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ToolsDialog extends BaseDialog {

    private final SetupTools tools;
    private final SourcesTab sources = new SourcesTab();

    private final JButton btnInstall = new  JButton(new AbstractAction("Install") {
        @Override
        public void actionPerformed(ActionEvent e) {
            Set<Tool> missing = EnumSet.noneOf(Tool.class);
            for (Map.Entry<Tool, ToolRow> entry : rows.entrySet()) {
                Tool tool = entry.getKey();
                ToolRow row = entry.getValue();
                if (!row.isInstalled()) {
                    missing.add(tool);
                }
            }
            runAction(
                actions -> actions.installMissing(missing),
                ToolsDialog.this::updateInstalledVersions
            );
        }
    });
    private final JButton btnCheckUpdates = new JButton(new AbstractAction("Check for updates") {
        @Override
        public void actionPerformed(ActionEvent e) {
            runAction(
                ToolActions::checkForUpdates,
                newVersions -> {
                    for (Map.Entry<Tool, String> entry : newVersions.entrySet()) {
                        Tool tool = entry.getKey();
                        String newVersion = entry.getValue();
                        rows.get(tool).setNewVersion(newVersion);
                    }
                }
            );
        }
    });

    private final Map<Tool, ToolRow> rows = new EnumMap<>(Tool.class);

    public ToolsDialog(ErrorLogger logger, Window owner, Tools tools) {
        super(owner, logger, "Tools setup");
        this.tools = SetupTools.create(tools);

        JPanel main = new JPanel(new BorderLayout());

        JPanel top = new JPanel();
        top.add(btnInstall);
        top.add(btnCheckUpdates);
        main.add(top, BorderLayout.NORTH);

        JPanel prows = new JPanel(new GridBagLayout());
        Tool[] toolList = Tool.values();
        for (int i = 0; i < toolList.length; i++) {
            Tool tool = toolList[i];
            ToolRow row = new ToolRow(tool);
            prows.add(row.lblName, new GridBagConstraints(
                0, i, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));
            prows.add(row.tfVersion, new GridBagConstraints(
                1, i, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
            ));
            prows.add(row.btnUpdate, new GridBagConstraints(
                2, i, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
            ));
            rows.put(tool, row);
            row.btnUpdate.addActionListener(e -> runAction(
                actions -> actions.update(tool),
                this::updateInstalledVersions)
            );
        }
        prows.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
        main.add(prows, BorderLayout.CENTER);

        JTabbedPane tab = new JTabbedPane();
        tab.add("Tool versions", main);
        tab.add("Advanced", sources.getVisual());
        add(tab, BorderLayout.CENTER);

        runAction(
            actions -> actions.getInstalledVersions(List.of(Tool.values())),
            this::updateInstalledVersions
        );

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void disableAll() {
        btnInstall.setEnabled(false);
        btnCheckUpdates.setEnabled(false);
        for (ToolRow row : rows.values()) {
            row.btnUpdate.setEnabled(false);
        }
    }

    private void enableRequired() {
        boolean anyNotInstalled = false;
        boolean anyInstalled = false;
        for (Map.Entry<Tool, ToolRow> entry : rows.entrySet()) {
            ToolRow row = entry.getValue();
            boolean installed = row.enableUpdate();
            if (installed) {
                anyInstalled = true;
            } else {
                anyNotInstalled = true;
            }
        }
        btnInstall.setEnabled(anyNotInstalled);
        btnCheckUpdates.setEnabled(anyInstalled);
    }

    private <T> void runAction(Function<ToolActions, T> action, Consumer<T> onSuccess) {
        disableAll();
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            LazyLogDialog log = new LazyLogDialog(this, Thread.currentThread());
            try {
                T maybeResult = null;
                ToolActions actions = new ToolActions(getLogger(), tools, sources, log);
                try {
                    maybeResult = action.apply(actions);
                } finally {
                    T result = maybeResult;
                    SwingUtilities.invokeLater(() -> {
                        log.close(actions.hasErrors());
                        if (result != null) {
                            onSuccess.accept(result);
                        }
                        setCursor(Cursor.getDefaultCursor());
                        enableRequired();
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> error(ex));
            }
        }).start();
    }

    private void updateInstalledVersions(Map<Tool, String> versions) {
        for (Map.Entry<Tool, String> entry : versions.entrySet()) {
            Tool tool = entry.getKey();
            String currentVersion = entry.getValue();
            rows.get(tool).setCurrentVersion(currentVersion);
        }
    }

    public static void fastCheckIfInstalled(ErrorLogger logger, Tools tools) {
        SetupTools setupTools = SetupTools.create(tools);
        if (setupTools.installed(Tool.PYTHON) && setupTools.installed(Tool.PIP) && setupTools.installed(Tool.FFMPEG)) {
            return;
        }
        new ToolsDialog(logger, null, setupTools);
    }
}
