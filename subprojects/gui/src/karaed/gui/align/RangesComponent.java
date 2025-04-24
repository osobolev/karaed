package karaed.gui.align;

import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;
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
import java.util.List;
import java.util.function.Consumer;

// todo: allow selection of ranges => make area of selected ranges???
// todo: allow manual edit of ranges??? but how it is compatible with range generation from params???
// todo: allow to edit areas
// todo: allow manual delete of ranges??? (or better mark area as empty?)
// todo: show text inside ranges???
// todo: better "currently playing" display
// todo: "go to": from lyrics to range, from range to lyrics
final class RangesComponent extends JComponent implements Scrollable {

    private final BaseWindow owner;
    private final ColorSequence colors;

    private final EditableRanges model;
    private final float frameRate;

    private final List<Runnable> playChangeListeners = new ArrayList<>();
    private final List<Consumer<AreaParams>> paramListeners = new ArrayList<>();

    private float pixPerSec = 30.0f;

    private Range playingRange = null;
    private Clip playing = null;

    private EditableArea editingArea = null;

    private SavedData beforeSplitting = null;

    private Integer dragStart = null;
    private Integer dragging = null;

    RangesComponent(BaseWindow owner, ColorSequence colors, EditableRanges model) {
        this.owner = owner;
        this.colors = colors;
        this.model = model;
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

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart != null && dragging != null) {
                    if (canEdit()) {
                        Measurer m = new Measurer(frameRate, pixPerSec);
                        int f1 = m.x2frame(dragStart.intValue());
                        int f2 = m.x2frame(dragging.intValue());
                        int from = Math.min(f1, f2);
                        int to = Math.max(f1, f2);
                        if (to > from) {
                            // todo: skip too small areas!!!
                            try {
                                model.addArea(model.newArea(from, to));
                            } catch (Exception ex) {
                                owner.error(ex);
                            }
                        }
                    }
                    dragStart = null;
                    dragging = null;
                    repaint();
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                Cursor c = Cursor.getDefaultCursor();
                if (model.getAreaCount() > 0 && beforeSplitting == null) {
                    Sizer s = newSizer();
                    int frame = s.x2frame(e.getX());
                    EditableArea area = s.findArea(frame, e.getY(), model);
                    if (area != null) {
                        c = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
                    }
                }
                if (getCursor() != c) {
                    setCursor(c);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!canEdit())
                    return;
                if (dragStart == null) {
                    dragStart = e.getX();
                }
                dragging = e.getX();
                repaint();
            }
        });
    }

    private Sizer newSizer() {
        return new Sizer(getFontMetrics(getFont()), frameRate, pixPerSec);
    }

    private int totalSeconds() {
        return (int) Math.ceil(model.source.frames() / frameRate);
    }

    private void doPaint(Painter painter) {
        painter.paint(colors, model, playingRange, editingArea);
    }

    private boolean canEdit() {
        return editingArea == null && beforeSplitting == null;
    }

    @Override
    protected void paintComponent(Graphics g) {
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
                doPaint(painter);
            }
            {
                g.setClip(x2, 0, width - x2, height);
                doPaint(painter);
            }
            g2.setComposite(composite);
            {
                g.setClip(x1, 0, x2 - x1, height);
                doPaint(painter);
            }
            g.setClip(null);
        } else {
            doPaint(painter);
        }

        if (dragStart != null && dragging != null) {
            painter.paintDrag(dragStart.intValue(), dragging.intValue());
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
            if (!canEdit())
                return;
            MenuBuilder menu = new MenuBuilder(me);
            menu.add(new AbstractAction("Add area & edit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Measurer m = new Measurer(frameRate, pixPerSec);
                    int delta = m.sec2frame(1);
                    EditableArea area = model.newAreaFromRange(range, delta);
                    if (area == null)
                        return;
                    try {
                        model.addArea(area);
                        editingArea = area;
                        fireParamsChanged();
                        repaint();
                    } catch (Exception ex) {
                        owner.error(ex);
                    }
                }
            });
            menu.showMenu();
        }
    }

    private void areaClicked(MouseEvent me, EditableArea area) {
        if (me.getButton() == MouseEvent.BUTTON1) {
            if (beforeSplitting != null)
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
            if (!canEdit())
                return;
            MenuBuilder menu = new MenuBuilder(me);
            menu.add(new AbstractAction("Remove area") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        model.removeArea(area);
                    } catch (Exception ex) {
                        owner.error(ex);
                    }
                }
            });
            menu.showMenu();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Sizer s = newSizer();
        return new Dimension(s.prefWidth(totalSeconds()), s.prefHeight());
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
        try {
            model.splitByParams(editingArea, params);
        } catch (Exception ex) {
            owner.error(ex);
        }
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
