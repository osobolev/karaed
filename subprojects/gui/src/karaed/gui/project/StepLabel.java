package karaed.gui.project;

import karaed.gui.ScaleUIDefaults;
import karaed.gui.components.toolbar.LinkLabel;
import karaed.gui.util.InputUtil;
import karaed.project.PipeStep;

import javax.swing.*;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StepLabel {

    private static final Icon RUNNING = InputUtil.getIcon("/running.png");
    private static final Icon COMPLETE = InputUtil.getIcon("/complete.png");
    private static final Icon ERROR = InputUtil.getIcon("/error.png");
    private static final Icon STALE = InputUtil.getIcon("/stale.png");

    private static final Color ACTIVE_COLOR = new Color(0, 0, 0);
    private static final Color INACTIVE_COLOR = new Color(150, 150, 150);
    private static final Color ACTIVE_LINK = new Color(0, 0, 150);
    private static final Color INACTIVE_LINK = new Color(150, 150, 200);

    private final PipeStep step;
    private final LinkLabel tpLabel;
    private final JLabel iconLabel = new JLabel();
    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private boolean hasVideo = true;

    StepLabel(PipeStep step, Consumer<LinkType> onClick) {
        this.step = step;
        this.tpLabel = new LinkLabel(panel, Font.DIALOG, ScaleUIDefaults.isize(24), e -> {
            String description = e.getDescription();
            try {
                LinkType link = LinkType.valueOf(description.substring(1).toUpperCase());
                onClick.accept(link);
            } catch (IllegalArgumentException ex) {
                // ignore
            }
        });
        panel.add(tpLabel.getVisual());
        panel.add(iconLabel);
    }

    private String getText(boolean active, boolean canLink) {
        String text = switch (step) {
        case DOWNLOAD -> hasVideo ? "Downloading [audio]#audio/[video]#video" : "Copying [audio]#audio";
        case DEMUCS -> "Separating vocals";
        case RANGES -> "Detecting [ranges]#ranges";
        case ALIGN -> "Aligning vocals with lyrics";
        case SUBS -> "Making [editable subtitles]#subs";
        case BACKVOCALS -> "Adding [backvocals]#backvocals";
        case KARAOKE -> "Making karaoke subtitles";
        case PREPARE_VIDEO -> "Preparing video";
        case VIDEO -> "Making [karaoke video]#karaoke";
        };
        StringBuilder buf = new StringBuilder();
        Pattern pattern = Pattern.compile("\\[([^]]+)]#([a-z]+)");
        Matcher matcher = pattern.matcher(text);
        Color linkColor = active ? ACTIVE_LINK : INACTIVE_LINK;
        while (matcher.find()) {
            String linkText = matcher.group(1);
            String href = matcher.group(2);
            String replacement;
            if (canLink || step == PipeStep.RANGES || step == PipeStep.BACKVOCALS) {
                replacement = LinkLabel.linkText(linkColor, "#" + href, linkText);
            } else {
                replacement = linkText;
            }
            matcher.appendReplacement(buf, replacement);
        }
        matcher.appendTail(buf);
        Color color = active ? ACTIVE_COLOR : INACTIVE_COLOR;
        return LinkLabel.labelText(color, buf);
    }

    private void setText(boolean active, boolean canLink, String tooltip) {
        tpLabel.setText(getText(active, canLink));
        iconLabel.setToolTipText(tooltip);
    }

    void setState(RunStepState state) {
        tpLabel.getVisual().setToolTipText(null);
        if (state instanceof RunStepState.Done) {
            setText(true, true, null);
            iconLabel.setIcon(COMPLETE);
        } else if (state instanceof RunStepState.NotRan) {
            setText(false, false, null);
            iconLabel.setIcon(null);
        } else if (state instanceof RunStepState.MustRerun(String because)) {
            setText(false, true, because);
            iconLabel.setIcon(STALE);
        } else if (state instanceof RunStepState.Running) {
            setText(true, false, null);
            iconLabel.setIcon(RUNNING);
        } else if (state instanceof RunStepState.Error(String message)) {
            setText(false, false, message);
            iconLabel.setIcon(ERROR);
        }
    }

    void setHasVideo(boolean hasVideo) {
        this.hasVideo = hasVideo;
    }

    JComponent getVisual() {
        return panel;
    }
}
