package karaed.gui.components.lyrics;

import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Color;

public final class MyPainter extends DefaultHighlighter.DefaultHighlightPainter {

    public MyPainter(Color c) {
        super(c);
    }

    public static void removeMyHighlights(Highlighter hl) {
        for (Highlighter.Highlight h : hl.getHighlights()) {
            if (h.getPainter() instanceof MyPainter) {
                hl.removeHighlight(h);
            }
        }
    }
}
