package karaed.gui.components;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.BooleanSupplier;

public final class EditorButtons {

    private final JPanel butt = new JPanel();

    private boolean isContinue = false;

    public EditorButtons(boolean canContinue, Action actionSave, BooleanSupplier save, Runnable close) {
        butt.add(new JButton(actionSave));
        if (canContinue) {
            butt.add(new JButton(new AbstractAction("Save & continue") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (save.getAsBoolean()) {
                        isContinue = true;
                        close.run();
                    }
                }
            }));
        }
        butt.add(new JButton(new AbstractAction(canContinue ? "Cancel" : "Close") {
            @Override
            public void actionPerformed(ActionEvent e) {
                close.run();
            }
        }));
    }

    public JComponent getVisual() {
        return butt;
    }

    public boolean isContinue() {
        return isContinue;
    }
}
