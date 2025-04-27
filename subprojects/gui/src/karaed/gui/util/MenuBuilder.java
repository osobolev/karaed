package karaed.gui.util;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class MenuBuilder {

    private final MouseEvent me;
    private final List<Action> actions = new ArrayList<>();
    private Point showAt = null;

    public MenuBuilder(MouseEvent me) {
        this.me = me;
    }

    public void setShowAt(Point p) {
        this.showAt = p;
    }

    public void add(Action action) {
        actions.add(action);
    }

    public void add(String text, Runnable action) {
        actions.add(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    public JPopupMenu showMenu(Runnable onClose) {
        if (actions.isEmpty())
            return null;
        JPopupMenu menu = new JPopupMenu();
        for (Action action : actions) {
            menu.add(action);
        }
        if (onClose != null) {
            menu.addPopupMenuListener(new PopupMenuListener() {

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    onClose.run();
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                }
            });
        }
        if (showAt != null) {
            menu.show(me.getComponent(), showAt.x, showAt.y);
        } else {
            menu.show(me.getComponent(), me.getX() - 5, me.getY() - 5);
        }
        return menu;
    }

    public JPopupMenu showMenu() {
        return showMenu(null);
    }
}
