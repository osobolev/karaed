package karaed.gui.start;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.function.Consumer;

final class RecentItem {

    private static final Color NORMAL_BG = Color.white;
    private static final Color ROLLOVER_BG = new Color(200, 240, 255);

    private static final Color NORMAL_FG = new Color(150, 150, 150);
    private static final Color BAD_FG = new Color(255, 120, 120);

    final Path dir;

    private final JPanel main = new JPanel();
    private final JLabel lblTitle = new JLabel("-");
    private final JLabel lblDir = new JLabel(" ");

    RecentItem(Path dir, Consumer<Path> listener) {
        this.dir = dir;

        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        lblTitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblTitle.getPreferredSize().height));
        lblDir.setMaximumSize(new Dimension(Integer.MAX_VALUE, lblDir.getPreferredSize().height));

        lblDir.setFont(lblDir.getFont().deriveFont(Font.PLAIN));
        lblDir.setForeground(NORMAL_FG);
        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.PLAIN));
        main.add(lblTitle);
        main.add(lblDir);
        main.setBackground(NORMAL_BG);
        main.setBorder(BorderFactory.createCompoundBorder(
            ItemBorder.INSTANCE,
            BorderFactory.createEmptyBorder(3, 5, 3, 5)
        ));
        main.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        main.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    listener.accept(dir);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                main.setBackground(ROLLOVER_BG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                main.setBackground(NORMAL_BG);
            }
        });

        lblDir.setText(dir.toString());
    }

    void updateInfo(boolean ok, String title) {
        if (!ok) {
            lblDir.setForeground(BAD_FG);
        }
        if (title != null) {
            lblTitle.setText(title);
        }
    }

    JComponent getVisual() {
        return main;
    }

    private static final class ItemBorder extends AbstractBorder {

        static final ItemBorder INSTANCE = new ItemBorder();

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(Color.lightGray);
            g.drawLine(x, y + height - 1, x + width - 1, y + height - 1);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 1, 0);
        }
    }
}
