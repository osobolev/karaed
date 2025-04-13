package karaed.gui.project;

import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.Font;

final class StepLabel {

    private static final Icon RUNNING = InputUtil.getIcon("/running.png");
    private static final Icon COMPLETE = InputUtil.getIcon("/complete.png");

    private final JLabel label;

    StepLabel(PipeStep step) {
        this.label = new JLabel(step.text);
        label.setFont(label.getFont().deriveFont(20f));
        label.setHorizontalTextPosition(JLabel.LEFT);
    }

    void setState(StepState state) {
        switch (state) {
        case INIT:
            label.setFont(label.getFont().deriveFont(Font.PLAIN));
            label.setIcon(null);
            break;
        case RUNNING:
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setIcon(RUNNING);
            break;
        case COMPLETE:
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setIcon(COMPLETE);
            break;
        }
    }

    JComponent getVisual() {
        return label;
    }
}
