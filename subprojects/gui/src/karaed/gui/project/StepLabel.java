package karaed.gui.project;

import karaed.gui.util.InputUtil;
import karaed.project.PipeStep;

import javax.swing.*;
import java.awt.Color;

final class StepLabel {

    private static final Icon RUNNING = InputUtil.getIcon("/running.png");
    private static final Icon COMPLETE = InputUtil.getIcon("/complete.png");
    private static final Icon ERROR = InputUtil.getIcon("/error.png");
    private static final Icon STALE = InputUtil.getIcon("/stale.png");

    private static final Color NOT_RAN_COLOR = new Color(150, 150, 150);

    private final JLabel label;

    StepLabel(PipeStep step) {
        String text = switch (step) {
        case DOWNLOAD -> "Downloading audio/video";
        case DEMUCS -> "Separating vocals";
        case RANGES -> "Detecting ranges";
        case ALIGN -> "Aligning vocals with lyrics";
        case SUBS -> "Making editable subtitles";
        case KARAOKE -> "Making karaoke subtitles";
        case PREPARE_VIDEO -> "Preparing video";
        case VIDEO -> "Making karaoke video";
        };
        this.label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(20f));
        label.setHorizontalTextPosition(JLabel.LEFT);
    }

    void setState(RunStepState state) {
        label.setToolTipText(null);
        if (state instanceof RunStepState.Done) {
            label.setForeground(Color.black);
            label.setIcon(COMPLETE);
        } else if (state instanceof RunStepState.NotRan) {
            label.setForeground(NOT_RAN_COLOR);
            label.setIcon(null);
        } else if (state instanceof RunStepState.MustRerun(String because)) {
            label.setForeground(NOT_RAN_COLOR);
            label.setIcon(STALE);
            label.setToolTipText(because);
        } else if (state instanceof RunStepState.Running) {
            label.setForeground(Color.black);
            label.setIcon(RUNNING);
        } else if (state instanceof RunStepState.Error(String message)) {
            label.setForeground(Color.red);
            label.setIcon(ERROR);
            label.setToolTipText(message);
        }
    }

    JComponent getVisual() {
        return label;
    }
}
