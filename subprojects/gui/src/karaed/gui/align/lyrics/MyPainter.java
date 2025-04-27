package karaed.gui.align.lyrics;

import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Color;

final class MyPainter extends DefaultHighlighter.DefaultHighlightPainter {

    MyPainter(Color c) {
        super(c);
    }

    static void removeMyHighlights(Highlighter hl) {
        for (Highlighter.Highlight h : hl.getHighlights()) {
            if (h.getPainter() instanceof MyPainter) {
                hl.removeHighlight(h);
            }
        }
    }
}
