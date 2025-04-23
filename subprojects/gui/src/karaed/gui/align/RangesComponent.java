package karaed.gui.align;

import karaed.engine.formats.ranges.Area;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;
import karaed.gui.ErrorLogger;
import karaed.gui.align.model.EditableRanges;
import karaed.gui.util.ShowMessage;

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
// todo: when editing params (of all/area), show changes and allow to commit/rollback them
// todo: allow to edit areas
// todo: allow manual delete of ranges??? (or better mark area as empty?)
// todo: show text inside ranges???
// todo: better "currently playing" display
// todo: "go to": from lyrics to range, from range to lyrics
final class RangesComponent extends JComponent implements Scrollable {

    private final ErrorLogger logger;
    private final ColorSequence colors;

    private final EditableRanges model;
    private final float frameRate;

    private final List<Runnable> playChangeListeners = new ArrayList<>();
    private final List<Consumer<AreaParams>> paramListeners = new ArrayList<>();

    private float pixPerSec = 30.0f;

    private Range playingRange = null;
    private Clip playing = null;

    private Area editingArea = null;

    private boolean splitInProgress = false;

    private Integer dragStart = null;
    private Integer dragging = null;

    RangesComponent(ErrorLogger logger, ColorSequence colors, EditableRanges model) {
        this.logger = logger;
        this.colors = colors;
        this.model = model;
        this.frameRate = model.source.format.getFrameRate();

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
                            model.addArea(new Area(from, to, model.getParams()));
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
                if (model.getAreaCount() > 0 && !splitInProgress) {
                    Sizer s = newSizer();
                    int frame = s.x2frame(e.getX());
                    int iarea = s.findArea(frame, e.getY(), model.getAreas());
                    if (iarea >= 0) {
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
        return (int) Math.ceil(model.source.frames / frameRate);
    }

    private void doPaint(Painter painter) {
        painter.paint(colors, model, playingRange, editingArea);
    }

    private boolean canEdit() {
        return editingArea == null && !splitInProgress;
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

        List<Range> ranges = model.getRanges();
        int irange = s.findRange(frame, me.getY(), ranges);
        if (irange >= 0) {
            Range range = ranges.get(irange);
            rangeClicked(me, range);
            return;
        }

        List<Area> areas = model.getAreas();
        int iarea = s.findArea(frame, me.getY(), areas);
        if (iarea >= 0) {
            Area area = areas.get(iarea);
            areaClicked(me, area);
        }
    }

    private void rangeClicked(MouseEvent me, Range range) {
        if (me.getButton() == MouseEvent.BUTTON1) {
            stop();
            try {
                Clip clip = model.source.source.open(range.from(), range.to());
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
                ShowMessage.error(this, logger, ex);
            }
        } else if (me.getButton() == MouseEvent.BUTTON3) {
            if (!canEdit())
                return;
            JPopupMenu menu = new JPopupMenu();
            menu.add(new AbstractAction("Add area & edit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // todo
                }
            });
            menu.show(this, me.getX() - 5, me.getY() - 5);
        }
    }

    private void areaClicked(MouseEvent me, Area area) {
        if (me.getButton() == MouseEvent.BUTTON1) {
            if (splitInProgress)
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
            JPopupMenu menu = new JPopupMenu();
            menu.add(new AbstractAction("Remove area") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    model.removeArea(area);
                }
            });
            menu.show(this, me.getX() - 5, me.getY() - 5);
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

    void fireParamsChanged() {
        AreaParams params = editingArea == null ? model.getParams() : editingArea.params();
        for (Consumer<AreaParams> listener : paramListeners) {
            listener.accept(params);
        }
    }

    void addParamListener(Consumer<AreaParams> listener) {
        paramListeners.add(listener);
    }

    void setSplitInProgress(boolean splitInProgress) {
        this.splitInProgress = splitInProgress;
    }

    void setParams(AreaParams params) {
        try {
            if (editingArea == null) {
                model.splitByParams(params);
            } else {
                // todo: change area params
            }
        } catch (Exception ex) {
            ShowMessage.error(this, logger, ex);
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        Dimension size = getPreferredSize();
        return new Dimension(1000, size.height);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10; // todo
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 10; // todo
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
