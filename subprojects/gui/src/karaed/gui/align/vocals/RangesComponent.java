package karaed.gui.align.vocals;

import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;
import karaed.gui.components.ColorSequence;
import karaed.gui.components.model.EditableArea;
import karaed.gui.components.model.EditableRanges;
import karaed.gui.components.model.RangeSide;
import karaed.gui.components.music.Measurer;
import karaed.gui.components.music.MusicComponent;
import karaed.gui.components.music.Painter;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.MenuBuilder;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;

// todo: allow selection of ranges => make area of selected ranges???
// todo: allow manual edit of ranges??? but how it is compatible with range generation from params???
// todo: allow manual delete of ranges??? (or better mark area as empty?)
public final class RangesComponent extends MusicComponent {

    /**
     * Background color for everything outside of edited area
     */
    private static final Color NOT_AREA_BG = new Color(120, 120, 120, 120);

    private final Consumer<Boolean> finishSplit;

    private final List<Consumer<AreaParams>> paramListeners = new ArrayList<>();
    private final List<AreaListener> areaListeners = new ArrayList<>();

    private EditableArea editingArea = null;

    private SavedData beforeSplitting = null;

    private Integer dragStart = null;
    private Integer dragEnd = null;
    private EditableArea resizingArea = null;
    private RangeSide resizeSide = null;
    private Integer draggingBorder = null;

    public RangesComponent(BaseWindow owner, ColorSequence colors, EditableRanges model,
                           Consumer<Boolean> finishSplit, IntFunction<String> getText) {
        super(owner, colors, model, getText);
        this.finishSplit = finishSplit;

        model.addListener(rangesChanged -> {
            if (rangesChanged) {
                stop();
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                mouseClick(e);
            }

            private static boolean areaTooSmall(Measurer m, int from, int to) {
                int frames = to - from;
                if (frames <= 0)
                    return true;
                int min = m.sec2frame(1f);
                return frames < min;
            }

            private void createNewArea() {
                Measurer m = newMeasurer();
                int f1 = m.x2frame(dragStart.intValue());
                int f2 = m.x2frame(dragEnd.intValue());
                int from = Math.min(f1, f2);
                int to = Math.max(f1, f2);
                if (areaTooSmall(m, from, to))
                    return;
                EditableArea area = model.newArea(from, to);
                if (area != null) {
                    model.addArea(area);
                }
            }

            private void resizeArea() {
                Measurer m = newMeasurer();
                int newBorder = m.x2frame(draggingBorder.intValue());
                int from;
                int to;
                if (resizeSide == RangeSide.LEFT) {
                    from = newBorder;
                    to = resizingArea.to();
                } else {
                    from = resizingArea.from();
                    to = newBorder;
                }
                if (areaTooSmall(m, from, to))
                    return;
                model.resizeArea(resizingArea, from, to);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean wasDragging;
                if (dragStart != null && dragEnd != null) {
                    wasDragging = true;
                    if (canEdit()) {
                        createNewArea();
                    }
                } else if (draggingBorder != null) {
                    wasDragging = true;
                    if (canEdit()) {
                        resizeArea();
                    }
                } else {
                    wasDragging = false;
                }
                dragStart = null;
                dragEnd = null;
                draggingBorder = null;
                if (wasDragging) {
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {

            private static final int NEAR_BORDER = 5;

            private Cursor maybeNewCursor(MouseEvent e) {
                if (dragStart != null || draggingBorder != null) {
                    // When dragging show default cursor
                    return null;
                }
                if (model.getAreaCount() <= 0) {
                    // No areas to show cursor for
                    return null;
                }
                AreaSizer s = newSizer();
                int frame = s.x2frame(e.getX());
                EditableArea area = s.findArea(frame, e.getY(), model);
                if (isSplitting()) {
                    if (area != null && area == editingArea) {
                        // Can unselect area
                        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    } else {
                        // Areas are not editable & cannot select/unselect area until commit/rollback
                        return null;
                    }
                }
                if (area != null) {
                    boolean canToggle = editingArea == null || area == editingArea;
                    if (canToggle) {
                        // Can select/unselect area
                        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    }
                }
                if (canEdit()) {
                    int delta = s.pix2frame(NEAR_BORDER);
                    RangeSide side = model.isOnAreaBorder(frame, delta, null);
                    if (side != null) {
                        // Can resize area
                        return Cursor.getPredefinedCursor(side == RangeSide.LEFT ? Cursor.W_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR);
                    }
                }
                return null;
            }

            private Cursor getNewCursor(MouseEvent e) {
                Cursor c = maybeNewCursor(e);
                return c == null ? Cursor.getDefaultCursor() : c;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Cursor c = getNewCursor(e);
                if (getCursor() != c) {
                    setCursor(c);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!canEdit())
                    return;
                if (draggingBorder != null) {
                    draggingBorder = e.getX();
                } else if (dragStart != null) {
                    dragEnd = e.getX();
                } else {
                    Measurer m = newMeasurer();
                    int frame = m.x2frame(e.getX());
                    int delta = m.pix2frame(NEAR_BORDER);
                    EditableArea[] area = new EditableArea[1];
                    RangeSide side = model.isOnAreaBorder(frame, delta, area);
                    if (side != null) {
                        resizingArea = area[0];
                        resizeSide = side;
                        draggingBorder = e.getX();
                    } else {
                        dragStart = dragEnd = e.getX();
                    }
                }
                repaint();
            }
        });
    }

    @Override
    protected AreaSizer newSizer() {
        return new AreaSizer(getFontMetrics(getFont()), frameRate, pixPerSec);
    }

    private void doPaint(AreaPainter painter, boolean saveRangeIndexes) {
        painter.paintAreas(model, editingArea);
        paintRanges(painter, saveRangeIndexes);
    }

    @Override
    protected Painter doPaint(Graphics g, int width, int height) {
        AreaSizer s = newSizer();
        AreaPainter painter = new AreaPainter(g, s, height);

        if (editingArea != null) {
            int x1 = s.frame2x(editingArea.from());
            int x2 = s.frame2x(editingArea.to());

            g.setColor(NOT_AREA_BG);
            g.fillRect(0, 0, x1, height);
            g.fillRect(x2, 0, width - x2, height);
            g.setColor(Color.gray);
            g.drawLine(x1, 0, x1, height);
            g.drawLine(x2, 0, x2, height);

            Graphics2D g2 = (Graphics2D) g;
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            Shape clip = g2.getClip();
            {
                g2.clipRect(0, 0, x1, height);
                doPaint(painter, false);
                g2.setClip(clip);
            }
            {
                g2.clipRect(x2, 0, width - x2, height);
                doPaint(painter, false);
                g2.setClip(clip);
            }
            g2.setComposite(composite);
            {
                g2.clipRect(x1, 0, x2 - x1, height);
                doPaint(painter, true);
                g2.setClip(clip);
            }
        } else {
            doPaint(painter, true);
        }

        if (dragStart != null && dragEnd != null) {
            painter.paintDrag(dragStart.intValue());
            painter.paintDrag(dragEnd.intValue());
        }
        if (draggingBorder != null) {
            painter.paintDrag(draggingBorder.intValue());
        }

        return painter;
    }

    private boolean canEdit() {
        return editingArea == null && !isSplitting();
    }

    private void mouseClick(MouseEvent me) {
        AreaSizer s = newSizer();
        int frame = s.x2frame(me.getX());

        if (rangeMouseClick(me, s, frame))
            return;

        EditableArea area = s.findArea(frame, me.getY(), model);
        if (area != null) {
            areaClicked(me, area);
        }
    }

    @Override
    protected void addRangeMenu(MenuBuilder menu, Range range) {
        if (canEdit()) {
            Measurer m = newMeasurer();
            int delta = m.sec2frame(1);
            EditableArea area = model.newAreaFromRange(range, delta);
            if (area != null) {
                menu.add("Add area & edit", () -> {
                    model.addArea(area);
                    selectArea(area, true);
                });
            }
        }
    }

    private void areaClicked(MouseEvent me, EditableArea area) {
        MenuBuilder menu = new MenuBuilder(me);
        if (me.getButton() == MouseEvent.BUTTON1) {
            if (!isSplitting()) {
                if (editingArea != null) {
                    if (editingArea == area) {
                        selectArea(null, false);
                    }
                } else {
                    selectArea(area, true);
                }
            } else {
                showSplittingMenu(menu, area);
            }
        } else {
            if (!isSplitting()) {
                if (editingArea != null) {
                    if (editingArea == area) {
                        menu.add("Unselect area", () -> selectArea(null, false));
                    }
                } else {
                    menu.add("Select area & edit", () -> selectArea(area, true));
                }
            } else {
                showSplittingMenu(menu, area);
            }
            if (canEdit()) {
                menu.add("Remove area", () -> model.removeArea(area));
            }
        }
        menu.showMenu();
    }

    private void showSplittingMenu(MenuBuilder menu, EditableArea area) {
        if (editingArea == area) {
            menu.add("Commit & unselect area", () -> endSplitting(true));
            menu.add("Rollback & unselect area", () -> endSplitting(false));
        }
    }

    private void endSplitting(boolean commit) {
        finishSplit.accept(commit);
        selectArea(null, false);
    }

    private void selectArea(EditableArea area, boolean forEdit) {
        editingArea = area;
        fireParamsChanged();
        fireAreaSelected(forEdit);
        repaint();
    }

    private AreaParams getModelParams() {
        return editingArea == null ? model.getParams() : editingArea.params();
    }

    public void fireParamsChanged() {
        AreaParams params = getModelParams();
        for (Consumer<AreaParams> listener : paramListeners) {
            listener.accept(params);
        }
    }

    public void addParamListener(Consumer<AreaParams> listener) {
        paramListeners.add(listener);
    }

    public boolean isAreaSelected() {
        return editingArea != null;
    }

    private void fireAreaSelected(boolean forEdit) {
        for (AreaListener listener : areaListeners) {
            listener.areaSelected(forEdit);
        }
    }

    public void addAreaListener(AreaListener listener) {
        areaListeners.add(listener);
    }

    public void startSplitting() {
        this.beforeSplitting = new SavedData(
            getModelParams(), model.getRanges().stream().toList()
        );
    }

    public void finishSplitting() {
        this.beforeSplitting = null;
    }

    public boolean isSplitting() {
        return beforeSplitting != null;
    }

    public void setParams(AreaParams params) {
        model.splitByParams(editingArea, params);
    }

    public void rollbackChanges() {
        if (beforeSplitting == null)
            return;
        model.setRangesSilent(editingArea, beforeSplitting.params(), beforeSplitting.ranges());
        repaint();
        fireParamsChanged();
    }
}
