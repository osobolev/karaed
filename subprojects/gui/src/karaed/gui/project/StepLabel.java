package karaed.gui.project;

import karaed.gui.util.InputUtil;
import karaed.project.PipeStep;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StepLabel {

    private static final Icon RUNNING = InputUtil.getIcon("/running.png");
    private static final Icon COMPLETE = InputUtil.getIcon("/complete.png");
    private static final Icon ERROR = InputUtil.getIcon("/error.png");
    private static final Icon STALE = InputUtil.getIcon("/stale.png");

    private static final Color NOT_RAN_COLOR = new Color(150, 150, 150);

    private final PipeStep step;
    private final JTextPane tpLabel = new JTextPane();
    private final JLabel iconLabel = new JLabel();
    private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));

    private static String toHex(int x) {
        String str = Integer.toString(x, 16);
        if (str.length() < 2) {
            return "0" + str;
        } else {
            return str;
        }
    }

    private static String toHtml(Color color) {
        return "#" + toHex(color.getRed()) + toHex(color.getGreen()) + toHex(color.getBlue());
    }

    StepLabel(PipeStep step, Consumer<LinkType> onClick) {
        this.step = step;
        tpLabel.setEditable(false);
        tpLabel.setFocusable(false);
        tpLabel.setBorder(BorderFactory.createEmptyBorder());
        tpLabel.setBackground(panel.getBackground());
        StyleSheet ss = new StyleSheet();
        String htmlColor = toHtml(panel.getBackground());
        ss.addRule(
            String.format(
                """
                    body {
                        font-family: Dialog;
                        font-size: 24;
                        background-color: %s;
                    }
                    """,
                htmlColor
            )
        );
        tpLabel.setEditorKit(new HTMLEditorKit() {
            @Override
            public StyleSheet getStyleSheet() {
                return ss;
            }
        });
        tpLabel.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                String description = e.getDescription();
                try {
                    LinkType link = LinkType.valueOf(description.substring(1).toUpperCase());
                    onClick.accept(link);
                } catch (IllegalArgumentException ex) {
                    // ignore
                }
            }
        });
        panel.add(tpLabel);
        panel.add(iconLabel);
    }

    // todo: for video link can be unavailable!!!
    private String getText(Color color, boolean canLink) {
        String text = switch (step) {
        case DOWNLOAD -> "Downloading [audio]#audio/[video]#video";
        case DEMUCS -> "Separating vocals";
        case RANGES -> "Detecting [ranges]#ranges";
        case ALIGN -> "Aligning vocals with lyrics";
        case SUBS -> "Making [editable subtitles]#subs";
        case KARAOKE -> "Making karaoke subtitles";
        case PREPARE_VIDEO -> "Preparing video";
        case VIDEO -> "Making [karaoke video]#karaoke";
        };
        StringBuilder buf = new StringBuilder();
        Pattern pattern = Pattern.compile("\\[([^]]+)]#([a-z]+)");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String linkText = matcher.group(1);
            String href = matcher.group(2);
            String replacement;
            if (canLink || step == PipeStep.RANGES) {
                replacement = String.format(
                    "<u><a href='#%s'>%s</a></u>",
                    href, linkText
                );
            } else {
                replacement = linkText;
            }
            matcher.appendReplacement(buf, replacement);
        }
        matcher.appendTail(buf);
        return "<html><font color='" + toHtml(color) + "'><b>" + buf + "</b></font></html>";
    }

    private void setText(Color color, boolean canLink) {
        tpLabel.setText(getText(color, canLink));
    }

    void setState(RunStepState state) {
        tpLabel.setToolTipText(null);
        if (state instanceof RunStepState.Done) {
            setText(Color.black, true);
            iconLabel.setIcon(COMPLETE);
        } else if (state instanceof RunStepState.NotRan) {
            setText(NOT_RAN_COLOR, false);
            iconLabel.setIcon(null);
        } else if (state instanceof RunStepState.MustRerun(String because)) {
            setText(NOT_RAN_COLOR, true);
            iconLabel.setIcon(STALE);
            tpLabel.setToolTipText(because);
        } else if (state instanceof RunStepState.Running) {
            setText(Color.black, false);
            iconLabel.setIcon(RUNNING);
        } else if (state instanceof RunStepState.Error(String message)) {
            setText(Color.red, false);
            iconLabel.setIcon(ERROR);
            tpLabel.setToolTipText(message);
        }
    }

    JComponent getVisual() {
        return panel;
    }
}
