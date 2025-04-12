package karaed.gui.start;

import karaed.engine.formats.info.Info;
import karaed.gui.ErrorLogger;
import karaed.gui.options.OptionsDialog;
import karaed.gui.project.ProjectFrame;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;
import karaed.gui.util.TitleUtil;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class StartFrame extends JFrame {

    private final ErrorLogger logger;
    private final Tools tools;
    private final Path rootDir;

    public StartFrame(ErrorLogger logger, Tools tools, Path rootDir) {
        super("KaraEd");
        this.logger = logger;
        this.tools = tools;
        this.rootDir = rootDir;

        JButton btnNew = new JButton(new AbstractAction("New project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OptionsDialog dlg = new OptionsDialog(logger, StartFrame.this, null);
                    Workdir workDir = dlg.getWorkDir();
                    if (workDir == null)
                        return;
                    openProject(workDir);
                } catch (Exception ex) {
                    ShowMessage.error(StartFrame.this, logger, ex);
                }
            }
        });
        JButton btnOpen = new JButton(new AbstractAction("Open project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                File file = InputUtil.chooseFile(
                    StartFrame.this, new FileFilter() {

                        @Override
                        public boolean accept(File f) {
                            if (f.isFile()) {
                                return "input.json".equals(f.getName());
                            } else {
                                return true;
                            }
                        }

                        @Override
                        public String getDescription() {
                            return "KaraEd projects";
                        }
                    }
                );
                if (file == null)
                    return;
                openProject(new Workdir(file.toPath().getParent()));
            }
        });
        JPanel top = new JPanel();
        top.add(btnNew);
        top.add(btnOpen);
        add(top, BorderLayout.NORTH);

        List<Path> recent = RecentItems.loadRecentItems(logger);

        JPanel rip = new JPanel();
        rip.setLayout(new BoxLayout(rip, BoxLayout.Y_AXIS));
        Map<Path, RecentItem> ripMap = new LinkedHashMap<>();
        Consumer<Path> listener = dir -> {
            if (!openProject(new Workdir(dir))) {
                // todo: merge error dialog with confirm dialog
                if (!ShowMessage.confirm2(this, "Remove this project from list?"))
                    return;
                RecentItems.removeRecentItem(logger, dir);
                RecentItem recentItem = ripMap.remove(dir);
                if (recentItem != null) {
                    rip.remove(recentItem.getVisual());
                    rip.revalidate();
                }
            }
        };
        for (Path dir : recent) {
            RecentItem itemPanel = new RecentItem(dir, listener);
            rip.add(itemPanel.getVisual());
            ripMap.put(dir, itemPanel);
        }
        JScrollPane sp = new JScrollPane(rip, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setPreferredSize(new Dimension(600, 400));
        add(sp, BorderLayout.CENTER);

        new Thread(() -> {
            for (RecentItem itemPanel : ripMap.values()) {
                Workdir workDir = new Workdir(itemPanel.dir);
                boolean exists = RecentItems.isProjectDir(workDir) == null;
                Info info = TitleUtil.getInfo(workDir);
                String title = info == null ? null : info.toString();
                SwingUtilities.invokeLater(() -> itemPanel.updateInfo(exists, title));
            }
        }).start();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private boolean openProject(Workdir workDir) {
        ProjectFrame pf = ProjectFrame.create(logger, this, tools, rootDir, workDir);
        if (pf == null)
            return false;
        dispose();
        pf.setVisible(true);
        return true;
    }
}
