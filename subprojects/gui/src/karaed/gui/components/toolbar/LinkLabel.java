package karaed.gui.components.toolbar;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.Color;
import java.awt.Font;
import java.util.function.Consumer;

public final class LinkLabel {

    private final JTextPane tpLabel = new JTextPane();

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

    public LinkLabel(JComponent parent, String fontFamily, int fontSize, Consumer<HyperlinkEvent> onClick) {
        tpLabel.setEditable(false);
        tpLabel.setFocusable(false);
        tpLabel.setBorder(BorderFactory.createEmptyBorder());
        tpLabel.setBackground(parent.getBackground());
        StyleSheet ss = new StyleSheet();
        String htmlColor = toHtml(parent.getBackground());
        ss.addRule(
            String.format(
                """
                    body {
                        font-family: %s;
                        font-size: %s;
                        background-color: %s;
                    }
                    """,
                fontFamily, fontSize, htmlColor
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
                onClick.accept(e);
            }
        });
    }

    public static LinkLabel create(JComponent parent, Consumer<HyperlinkEvent> onClick) {
        Font labelFont = UIManager.getFont("Label.font");
        return new LinkLabel(parent, labelFont.getFontName(), labelFont.getSize(), onClick);
    }

    public static String linkText(Color linkColor, String href, String linkText) {
        return String.format(
            "<font color='%s'><u><a href='%s'>%s</a></u></font>",
            toHtml(linkColor), href, linkText
        );
    }

    public static String linkText(String href, String linkText) {
        return linkText(Color.blue, href, linkText);
    }

    public static String labelText(Color color, CharSequence text) {
        if (color == null) {
            return "<html><b>" + text + "</b></html>";
        } else {
            return "<html><font color='" + toHtml(color) + "'><b>" + text + "</b></font></html>";
        }
    }

    public void setText(String text) {
        tpLabel.setText(text);
    }

    public JComponent getVisual() {
        return tpLabel;
    }
}
