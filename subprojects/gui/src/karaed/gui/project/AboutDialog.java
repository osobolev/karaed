package karaed.gui.project;

import karaed.gui.util.BaseDialog;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.InputUtil;
import karaed.gui.util.VerticalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

final class AboutDialog extends BaseDialog {

    AboutDialog(BaseWindow owner) {
        super(owner, "About");

        String version = System.getProperty("jpackage.app-version");
        JPanel phead = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        phead.add(new JLabel(InputUtil.getIcon("/karaed.png")));
        JLabel lblHeader = new JLabel("KaraEd" + (version == null ? "" : " " + version));
        Font defFont = lblHeader.getFont();
        lblHeader.setFont(defFont.deriveFont(16f));
        phead.add(lblHeader);

        JPanel pdesc = new JPanel(new BorderLayout());
        JLabel lblDesc = new JLabel("Karaoke Editor", JLabel.CENTER);
        lblDesc.setFont(defFont.deriveFont(24f));
        pdesc.add(lblDesc, BorderLayout.CENTER);

        JPanel plink = new JPanel(new BorderLayout());
        LinkLabel link = new LinkLabel(plink, defFont.getFamily(), defFont.getSize(), e -> {
            try {
                Desktop.getDesktop().browse(e.getURL().toURI());
            } catch (Exception ex) {
                error(ex);
            }
        });
        String url = "https://github.com/osobolev/karaed";
        link.setText(LinkLabel.labelText(null, "GitHub: " + LinkLabel.linkText(Color.blue, url, url)));
        plink.add(link.getVisual());
        plink.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel pjava = new JPanel(new BorderLayout());
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024L / 1024L;
        pjava.add(new JLabel("Java: " + Runtime.version() + ", max memory: " + maxMemory + " Mb", JLabel.CENTER));
        pjava.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JPanel main = new JPanel(new VerticalLayout());
        main.add(phead);
        main.add(pdesc);
        main.add(plink);
        main.add(pjava);
        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        add(main, BorderLayout.CENTER);

        JButton btnOk = new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        JPanel butt = new JPanel();
        butt.add(btnOk);
        add(butt, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }
}
