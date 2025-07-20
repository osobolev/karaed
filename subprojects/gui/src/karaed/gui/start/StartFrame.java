package karaed.gui.start;

import karaed.engine.formats.info.Info;
import karaed.gui.ErrorLogger;
import karaed.gui.options.OptionsDialog;
import karaed.gui.project.ProjectFrame;
import karaed.gui.util.BaseFrame;
import karaed.gui.util.InputUtil;
import karaed.gui.util.TitleUtil;
import karaed.project.Workdir;
import karaed.tools.Tools;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class StartFrame extends BaseFrame {

    private final Tools tools;
    private final Path rootDir;

    public StartFrame(ErrorLogger logger, Tools tools, Path rootDir) {
        super(logger, "KaraEd");
        this.tools = tools;
        this.rootDir = rootDir;

        JButton btnNew = new JButton(new AbstractAction("New project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    OptionsDialog dlg = OptionsDialog.newProject(logger, StartFrame.this, null, null);
                    Workdir workDir = dlg.getWorkDir();
                    if (workDir == null)
                        return;
                    openProject(workDir);
                } catch (Exception ex) {
                    error(ex);
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
        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        top.add(btnNew);
        top.add(btnOpen);
        add(top, BorderLayout.NORTH);

        List<Path> recent = RecentItems.loadRecentItems(logger);

        JPanel rip = new JPanel();
        rip.setLayout(new BoxLayout(rip, BoxLayout.Y_AXIS));
        Consumer<RecentItem> onDelete = item -> {
            Path dir = item.dir;
            RecentItems.removeRecentItem(logger, dir);
            rip.remove(item.getVisual());
            rip.revalidate();
        };
        Consumer<RecentItem> onClick = item -> {
            Workdir workDir = new Workdir(item.dir);
            openProject(workDir, error -> {
                if (!confirm2(error + ".\nRemove this project from list?"))
                    return;
                onDelete.accept(item);
            });
        };
        List<RecentItem> loadInfos = new ArrayList<>();
        for (Path dir : recent) {
            RecentItem itemPanel = new RecentItem(dir, onClick, onDelete);
            rip.add(itemPanel.getVisual());
            loadInfos.add(itemPanel);
        }
        JScrollPane sp = new JScrollPane(rip, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setPreferredSize(new Dimension(600, 400));
        add(sp, BorderLayout.CENTER);

        new Thread(() -> {
            for (RecentItem itemPanel : loadInfos) {
                Workdir workDir = new Workdir(itemPanel.dir);
                boolean exists = RecentItems.isProjectDir(workDir) == null;
                Info info = TitleUtil.getInfo(workDir);
                String title = info == null ? null : info.toString();
                SwingUtilities.invokeLater(() -> itemPanel.updateInfo(exists, title));
            }
        }).start();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void openProject(Workdir workDir, Consumer<String> onError) {
        ProjectFrame pf = ProjectFrame.create(getLogger(), true, tools, rootDir, workDir, onError);
        if (pf == null)
            return;
        dispose();
        pf.setVisible(true);
    }

    private void openProject(Workdir workDir) {
        openProject(workDir, this::error);
    }
}
