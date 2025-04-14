package karaed.gui.project;

import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.Color;

final class StepLabel {

    private static final Icon RUNNING = InputUtil.getIcon("/running.png");
    private static final Icon COMPLETE = InputUtil.getIcon("/complete.png");

    private static final Color INIT_COLOR = new Color(150, 150, 150);

    private final JLabel label;

    StepLabel(PipeStep step) {
        this.label = new JLabel(step.text);
        label.setFont(label.getFont().deriveFont(20f));
        label.setHorizontalTextPosition(JLabel.LEFT);
    }

    void setState(StepState state) {
        switch (state) {
        case INIT:
            label.setForeground(INIT_COLOR);
            label.setIcon(null);
            break;
        case RUNNING:
            label.setForeground(Color.black);
            label.setIcon(RUNNING);
            break;
        case COMPLETE:
            label.setForeground(Color.black);
            label.setIcon(COMPLETE);
            break;
        }
    }

    JComponent getVisual() {
        return label;
    }
}
