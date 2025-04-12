package karaed.gui.project;

import javax.swing.*;
import java.awt.Font;

final class StepLabel {

    private static final Icon RUNNING = new ImageIcon(StepLabel.class.getResource("/running.png"));
    private static final Icon COMPLETE = new ImageIcon(StepLabel.class.getResource("/complete.png"));

    private final JLabel label;

    StepLabel(PipeStep step) {
        this.label = new JLabel(step.text);
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
