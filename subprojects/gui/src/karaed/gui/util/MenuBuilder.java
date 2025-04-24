package karaed.gui.util;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class MenuBuilder {

    private final MouseEvent me;
    private final List<Action> actions = new ArrayList<>();

    public MenuBuilder(MouseEvent me) {
        this.me = me;
    }

    public void add(Action action) {
        actions.add(action);
    }

    public void showMenu(Runnable onClose) {
        if (actions.isEmpty())
            return;
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
        menu.show(me.getComponent(), me.getX() - 5, me.getY() - 5);
    }

    public void showMenu() {
        showMenu(null);
    }
}
