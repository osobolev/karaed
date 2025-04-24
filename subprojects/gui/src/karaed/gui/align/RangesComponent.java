package karaed.gui.align;

import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;
import karaed.gui.align.model.AreaSide;
import karaed.gui.align.model.EditableArea;
import karaed.gui.align.model.EditableRanges;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.MenuBuilder;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

// todo: allow selection of ranges => make area of selected ranges???
// todo: allow manual edit of ranges??? but how it is compatible with range generation from params???
// todo: allow manual delete of ranges??? (or better mark area as empty?)
// todo: better "currently playing" display
// todo: "go to": from lyrics to range, from range to lyrics
final class RangesComponent extends JComponent implements Scrollable {

    private final BaseWindow owner;
    private final ColorSequence colors;
    private final EditableRanges model;
    private final IntFunction<String> getText;
    private final IntConsumer goTo;
    private final float frameRate;

    private final List<Runnable> playChangeListeners = new ArrayList<>();
    private final List<Consumer<AreaParams>> paramListeners = new ArrayList<>();

    private float pixPerSec = 30.0f;

    private Range playingRange = null;
    private Clip playing = null;

    private final Map<Range, Integer> paintedRangeIndex = new HashMap<>();

    private EditableArea editingArea = null;

    private SavedData beforeSplitting = null;

    private Integer dragStart = null;
    private Integer dragEnd = null;
    private EditableArea resizingArea = null;
    private AreaSide resizeSide = null;
    private Integer draggingBorder = null;

    RangesComponent(BaseWindow owner, ColorSequence colors, EditableRanges model,
                    IntFunction<String> getText, IntConsumer goTo) {
        this.owner = owner;
        this.colors = colors;
        this.model = model;
        this.getText = getText;
        this.goTo = goTo;
        this.frameRate = model.source.frameRate();

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
                if (resizeSide == AreaSide.LEFT) {
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
                if (isSplitting()) {
                    // Areas are not editable & cannot select/unselect area until commit/rollback
                    return null;
                }
                if (model.getAreaCount() <= 0) {
                    // No areas to show cursor for
                    return null;
                }
                if (dragStart != null || draggingBorder != null) {
                    // When dragging show default cursor
                    return null;
                }
                Sizer s = newSizer();
                int frame = s.x2frame(e.getX());
                EditableArea area = s.findArea(frame, e.getY(), model);
                if (area != null) {
                    boolean canToggle = editingArea == null || area == editingArea;
                    if (canToggle) {
                        // Can select/unselect area
                        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    }
                }
                if (canEdit()) {
                    int delta = s.pix2frame(NEAR_BORDER);
                    AreaSide side = model.isOnAreaBorder(frame, delta, null);
                    if (side != null) {
                        // Can resize area
                        return Cursor.getPredefinedCursor(side == AreaSide.LEFT ? Cursor.W_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR);
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
                    AreaSide side = model.isOnAreaBorder(frame, delta, area);
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

        ToolTipManager.sharedInstance().registerComponent(this);
    }

    private Measurer newMeasurer() {
        return new Measurer(frameRate, pixPerSec);
    }

    private Sizer newSizer() {
        return new Sizer(getFontMetrics(getFont()), frameRate, pixPerSec);
    }

    private int totalSeconds() {
        return (int) Math.ceil(model.source.frames() / frameRate);
    }

    private void doPaint(Painter painter, Map<Range, Integer> rangeIndexes) {
        painter.paint(colors, model, playingRange, editingArea, rangeIndexes);
    }

    private boolean canEdit() {
        return editingArea == null && !isSplitting();
    }

    @Override
    protected void paintComponent(Graphics g) {
        paintedRangeIndex.clear();

        super.paintComponent(g);

        FontMetrics fm = getFontMetrics(getFont());
        int width = getWidth();
        int height = getHeight();
        Painter painter = new Painter(g, fm, frameRate, pixPerSec, height);
        painter.paintScale(totalSeconds(), width);

        if (editingArea != null) {
            int x1 = painter.frame2x(editingArea.from());
            int x2 = painter.frame2x(editingArea.to());

            g.setColor(new Color(120, 120, 120, 120)); // todo
            g.fillRect(0, 0, x1, height);
            g.fillRect(x2, 0, width - x2, height);
            g.setColor(Color.gray); // todo
            g.drawLine(x1, 0, x1, height);
            g.drawLine(x2, 0, x2, height);

            Graphics2D g2 = (Graphics2D) g;
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
            {
                g.setClip(0, 0, x1, height);
                doPaint(painter, null);
            }
            {
                g.setClip(x2, 0, width - x2, height);
                doPaint(painter, null);
            }
            g2.setComposite(composite);
            {
                g.setClip(x1, 0, x2 - x1, height);
                doPaint(painter, paintedRangeIndex);
            }
            g.setClip(null);
        } else {
            doPaint(painter, paintedRangeIndex);
        }

        if (dragStart != null && dragEnd != null) {
            painter.paintDrag(dragStart.intValue());
            painter.paintDrag(dragEnd.intValue());
        }
        if (draggingBorder != null) {
            painter.paintDrag(draggingBorder.intValue());
        }
    }

    private void mouseClick(MouseEvent me) {
        Sizer s = newSizer();
        int frame = s.x2frame(me.getX());

        Range range = s.findRange(frame, me.getY(), model);
        if (range != null) {
            rangeClicked(me, range);
            return;
        }

        EditableArea area = s.findArea(frame, me.getY(), model);
        if (area != null) {
            areaClicked(me, area);
        }
    }

    private void rangeClicked(MouseEvent me, Range range) {
        if (me.getButton() == MouseEvent.BUTTON1) {
            stop();
            try {
                Clip clip = model.source.open(range.from(), range.to());
                playingRange = range;
                playing = clip;
                clip.addLineListener(le -> {
                    if (le.getType() == LineEvent.Type.STOP) {
                        stop();
                    }
                });
                firePlayChanged();
                clip.start();
                repaint();
            } catch (Exception ex) {
                owner.error(ex);
            }
        } else if (me.getButton() == MouseEvent.BUTTON3) {
            MenuBuilder menu = new MenuBuilder(me);
            Measurer m = newMeasurer();
            int delta = m.sec2frame(1);
            EditableArea area = model.newAreaFromRange(range, delta);
            if (area != null) {
                menu.add(new AbstractAction("Add area & edit") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.addArea(area);
                        editingArea = area;
                        fireParamsChanged();
                        repaint();
                    }
                });
            }
            menu.showMenu();
        }
    }

    private void areaClicked(MouseEvent me, EditableArea area) {
        if (me.getButton() == MouseEvent.BUTTON1) {
            if (isSplitting())
                return;
            if (editingArea != null) {
                if (editingArea == area) {
                    editingArea = null;
                    fireParamsChanged();
                    repaint();
                }
            } else {
                editingArea = area;
                fireParamsChanged();
                repaint();
            }
        } else {
            MenuBuilder menu = new MenuBuilder(me);
            if (canEdit()) {
                menu.add(new AbstractAction("Remove area") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        model.removeArea(area);
                    }
                });
            }
            menu.showMenu();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Sizer s = newSizer();
        return new Dimension(s.prefWidth(totalSeconds()), s.prefHeight());
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Sizer s = newSizer();
        int frame = s.x2frame(e.getX());
        Range range = s.findRange(frame, e.getY(), model);
        Integer index = paintedRangeIndex.get(range);
        if (index == null)
            return null;
        return getText.apply(index.intValue());
    }

    private void firePlayChanged() {
        for (Runnable listener : playChangeListeners) {
            listener.run();
        }
    }

    void addPlayChangeListener(Runnable listener) {
        playChangeListeners.add(listener);
    }

    boolean isPlaying() {
        return playing != null;
    }

    void stop() {
        if (playing != null) {
            playing.stop();
            playingRange = null;
            playing = null;
            firePlayChanged();
            repaint();
        }
    }

    void setScale(float pixPerSec) {
        this.pixPerSec = pixPerSec;
        revalidate();
        repaint();
    }

    void recolor() {
        repaint();
    }

    AreaParams getModelParams() {
        return editingArea == null ? model.getParams() : editingArea.params();
    }

    void fireParamsChanged() {
        AreaParams params = getModelParams();
        for (Consumer<AreaParams> listener : paramListeners) {
            listener.accept(params);
        }
    }

    void addParamListener(Consumer<AreaParams> listener) {
        paramListeners.add(listener);
    }

    void startSplitting() {
        this.beforeSplitting = new SavedData(
            getModelParams(), model.getRanges().stream().toList()
        );
    }

    void finishSplitting() {
        this.beforeSplitting = null;
    }

    boolean isSplitting() {
        return beforeSplitting != null;
    }

    void setParams(AreaParams params) {
        model.splitByParams(editingArea, params);
    }

    void rollbackChanges() {
        if (beforeSplitting == null)
            return;
        model.setRangesSilent(editingArea, beforeSplitting.params(), beforeSplitting.ranges());
        repaint();
        fireParamsChanged();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Sizer s = newSizer();
        return new Dimension(1000, s.prefHeight());
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 50;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return 50;
        } else {
            return Math.max(visibleRect.width - 50, 50);
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return true;
    }
}
