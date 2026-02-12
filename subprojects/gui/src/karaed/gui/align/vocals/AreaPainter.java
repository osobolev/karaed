package karaed.gui.align.vocals;

import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;

import java.awt.Color;
import java.awt.Graphics;

final class AreaPainter extends Painter {

    /**
     * Background color for area
     */
    private static final Color AREA_BG = new Color(120, 120, 120, 120);
    /**
     * Background color for area action band
     */
    private static final Color ACTION_BAND_BG = Color.gray;

    private final AreaSizer s;

    AreaPainter(Graphics g, AreaSizer s, int height) {
        super(g, s, height);
        this.s = s;
    }

    void paintAreas(EditableRanges model, EditableArea editingArea) {
        int ya = s.areaEditY1();
        for (EditableArea area : model.getAreas()) {
            int x1 = s.frame2x(area.from());
            int x2 = s.frame2x(area.to());
            int width = Measurer.width(x1, x2);
            if (area != editingArea) {
                g.setColor(AREA_BG);
                g.fillRect(x1, 0, width, height);
            }
            g.setColor(ACTION_BAND_BG);
            g.fillRect(x1, ya, width, AreaSizer.AREA_EDIT_H);
            g.setColor(Color.darkGray);
            g.drawLine(x1, ya, x1 + width, ya);
            g.drawLine(x1, ya + AreaSizer.AREA_EDIT_H, x1 + width, ya + AreaSizer.AREA_EDIT_H);
        }
    }
}
