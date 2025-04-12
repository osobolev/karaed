package karaed.gui.start;

import karaed.gui.ErrorLogger;
import karaed.gui.options.OptionsDialog;
import karaed.gui.project.ProjectFrame;
import karaed.gui.util.InputUtil;
import karaed.gui.util.ShowMessage;
import karaed.tools.Tools;
import karaed.workdir.Workdir;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;

// todo: new file (show new project dialog with input & options)
// todo: load project
// todo: show recent projects
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

        // todo: show recent projects

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void openProject(Workdir workDir) {
        dispose();
        // todo: on close re-open start frame
        new ProjectFrame(logger, tools, rootDir, workDir);
    }
}
