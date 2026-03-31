package karaed.gui.components.toolbar;

import karaed.gui.tools.SetupTools;
import karaed.gui.tools.ToolsDialog;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.Insets;

public final class ToolButtons {

    public static JComponent create(BaseWindow owner, SetupTools tools) {
        JButton btnTools = new JButton(InputUtil.getIcon("/tools.png"));
        btnTools.addActionListener(e -> new ToolsDialog(owner.getLogger(), owner.toWindow(), false, tools));
        btnTools.setToolTipText("Tools setup");
        btnTools.setMargin(new Insets(0, 3, 0, 3));

        JButton btnAbout = new JButton(InputUtil.getIcon("/help.png"));
        btnAbout.addActionListener(e -> new AboutDialog(owner).setVisible(true));
        btnAbout.setToolTipText("About");
        btnAbout.setMargin(new Insets(0, 3, 0, 3));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.add(btnTools);
        panel.add(Box.createHorizontalStrut(5));
        panel.add(btnAbout);
        return panel;
    }
}
