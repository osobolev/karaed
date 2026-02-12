package karaed.gui.components.lyrics;

import karaed.gui.util.InputUtil;
import karaed.gui.util.MenuBuilder;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.*;

final class GutterComponent extends JComponent {

    private static final Icon GOTO = InputUtil.getIcon("/goto.png");

    private final JComponent main;

    private NavigableMap<Integer, Integer> ys = Collections.emptyNavigableMap();
    private final int h;

    private int currentMouseY = -1;
    private int currentLineY = -1;

    private JPopupMenu menu = null;

    private final List<LyricsClickListener> listeners = new ArrayList<>();

    GutterComponent(JComponent main) {
        this.main = main;
        this.h = GOTO.getIconHeight();

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                Map.Entry<Integer, Integer> entry = getLine(e.getY());
                if (entry == null)
                    return;
                int lineIndex = entry.getValue().intValue();
                if (e.getButton() == MouseEvent.BUTTON1) {
                    goTo(lineIndex);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    showMenu(e, entry.getKey().intValue(), lineIndex);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                currentMouseY = -1;
                if (menu != null)
                    return;
                moveMarkerToMouse();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                currentMouseY = e.getY();
                if (menu != null)
                    return;
                moveMarkerToMouse();
            }
        });
    }

    private void showMenu(MouseEvent e, int lineY, int lineIndex) {
        MenuBuilder menu = new MenuBuilder(e);
        menu.add("Show range", () -> goTo(lineIndex));
        menu.add("Play", () -> play(lineIndex));
        Point showAt = new Point(getIconX(), lineY + h / 2);
        menu.setShowAt(showAt);
        this.menu = menu.showMenu(() -> {
            if (this.menu == null)
                return;
            this.menu = null;
            moveMarkerToMouse();
        });
    }

    void addListener(LyricsClickListener listener) {
        listeners.add(listener);
    }

    private void goTo(int lineIndex) {
        for (LyricsClickListener listener : listeners) {
            listener.lineClicked(lineIndex, false);
        }
    }

    private void play(int lineIndex) {
        for (LyricsClickListener listener : listeners) {
            listener.lineClicked(lineIndex, true);
        }
    }

    private void removeMenu() {
        if (menu == null)
            return;
        JPopupMenu localMenu = menu;
        menu = null;
        localMenu.setVisible(false);
    }

    private void moveMarkerToMouse() {
        int y = currentMouseY;
        int newLineY = getLineY(y);
        if (newLineY != currentLineY) {
            scheduleRepaint(currentLineY);
            scheduleRepaint(newLineY);
            currentLineY = newLineY;
        }
    }

    private int getLineY(int y) {
        Map.Entry<Integer, Integer> entry = getLine(y);
        return entry == null ? -1 : entry.getKey().intValue();
    }

    private Map.Entry<Integer, Integer> getLine(int y) {
        if (y < 0)
            return null;
        Map.Entry<Integer, Integer> before = ys.floorEntry(y); // <= currentMouseY
        Map.Entry<Integer, Integer> after = ys.higherEntry(y); // > currentMouseY
        if (before != null && after != null) {
            int dy1 = Math.abs(before.getKey().intValue() - y);
            int dy2 = Math.abs(after.getKey().intValue() - y);
            if (dy1 < dy2) {
                return before;
            } else {
                return after;
            }
        } else if (before != null) {
            return before;
        } else if (after != null) {
            return after;
        } else {
            return null;
        }
    }

    private void scheduleRepaint(int y) {
        if (y < 0)
            return;
        repaint(0, y - h / 2, getWidth(), h + 1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (currentLineY >= 0) {
            GOTO.paintIcon(this, g, getIconX(), currentLineY - h / 2);
        }
    }

    private int getIconX() {
        return getWidth() - GOTO.getIconWidth() - 1;
    }

    void setLines(NavigableMap<Integer, Integer> ys) {
        if (!Objects.equals(this.ys, ys)) {
            this.ys = ys;
            currentLineY = getLineY(currentMouseY);
            removeMenu();
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(20, main.getPreferredSize().height);
    }
}
