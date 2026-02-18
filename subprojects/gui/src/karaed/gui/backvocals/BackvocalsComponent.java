package karaed.gui.backvocals;

import karaed.engine.formats.backvocals.BackRange;
import karaed.engine.formats.backvocals.Backvocals;
import karaed.engine.formats.ranges.Range;
import karaed.gui.components.ColorSequence;
import karaed.gui.components.model.BackvocalRanges;
import karaed.gui.components.model.EditableRanges;
import karaed.gui.components.model.RangeSide;
import karaed.gui.components.music.Measurer;
import karaed.gui.components.music.MusicComponent;
import karaed.gui.components.music.Painter;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.MenuBuilder;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

final class BackvocalsComponent extends MusicComponent {

    private final BackvocalRanges ranges;

    private Integer dragStart = null;
    private Integer dragEnd = null;
    private Range resizingRange = null;
    private RangeSide resizeSide = null;
    private Integer draggingBorder = null;

    BackvocalsComponent(BaseWindow owner, ColorSequence colors, EditableRanges model, BackvocalRanges ranges, Runnable onChange) {
        super(owner, colors, model);
        this.ranges = ranges;

        ranges.addListener(() -> {
            onChange.run();
            stop();
            repaint();
        });

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                mouseClick(e);
            }

            private static boolean rangeTooSmall(Measurer m, int from, int to) {
                int frames = to - from;
                if (frames <= 0)
                    return true;
                int min = m.sec2frame(1f);
                return frames < min;
            }

            private void createNewRange() {
                Measurer m = newMeasurer();
                int f1 = m.x2frame(dragStart.intValue());
                int f2 = m.x2frame(dragEnd.intValue());
                int from = Math.min(f1, f2);
                int to = Math.max(f1, f2);
                if (rangeTooSmall(m, from, to))
                    return;
                Range range = ranges.newRange(from, to);
                if (range != null) {
                    ranges.addRange(range);
                }
            }

            private void resizeRange() {
                Measurer m = newMeasurer();
                int newBorder = m.x2frame(draggingBorder.intValue());
                int from;
                int to;
                if (resizeSide == RangeSide.LEFT) {
                    from = newBorder;
                    to = resizingRange.to();
                } else {
                    from = resizingRange.from();
                    to = newBorder;
                }
                if (rangeTooSmall(m, from, to))
                    return;
                ranges.resizeRange(resizingRange, from, to);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean wasDragging;
                if (dragStart != null && dragEnd != null) {
                    wasDragging = true;
                    createNewRange();
                } else if (draggingBorder != null) {
                    wasDragging = true;
                    resizeRange();
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
                BackvocalSizer s = newSizer();
                int frame = s.x2frame(e.getX());
                int delta = s.pix2frame(NEAR_BORDER);
                RangeSide side = ranges.isOnRangeBorder(frame, delta, null);
                if (side != null) {
                    // Can resize range
                    return Cursor.getPredefinedCursor(side == RangeSide.LEFT ? Cursor.W_RESIZE_CURSOR : Cursor.E_RESIZE_CURSOR);
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
                if (draggingBorder != null) {
                    draggingBorder = e.getX();
                } else if (dragStart != null) {
                    dragEnd = e.getX();
                } else {
                    Measurer m = newMeasurer();
                    int frame = m.x2frame(e.getX());
                    int delta = m.pix2frame(NEAR_BORDER);
                    Range[] range = new Range[1];
                    RangeSide side = ranges.isOnRangeBorder(frame, delta, range);
                    if (side != null) {
                        resizingRange = range[0];
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
    protected BackvocalSizer newSizer() {
        return new BackvocalSizer(getFontMetrics(getFont()), frameRate, pixPerSec);
    }

    @Override
    protected Painter doPaint(Graphics g, int width, int height) {
        BackvocalSizer s = newSizer();
        BackvocalPainter painter = new BackvocalPainter(g, s, height);

        paintRanges(painter, true);
        painter.paintBackvocals(ranges);

        if (dragStart != null && dragEnd != null) {
            painter.paintDrag(dragStart.intValue());
            painter.paintDrag(dragEnd.intValue());
        }
        if (draggingBorder != null) {
            painter.paintDrag(draggingBorder.intValue());
        }

        return painter;
    }

    private void mouseClick(MouseEvent me) {
        BackvocalSizer s = newSizer();
        int frame = s.x2frame(me.getX());

        if (rangeMouseClick(me, s, frame, this::addRangeMenu))
            return;

        Range backRange = s.findBackRange(frame, me.getY(), ranges);
        if (backRange != null) {
            rangeClicked(
                me, backRange, s.backSeekY1(),
                menu -> menu.add("Remove backvocals", () -> ranges.removeRange(backRange))
            );
        }
    }

    private void addRangeMenu(MenuBuilder menu, Range range) {
        menu.add("Create backvocals for range", () -> {
            Range newRange = ranges.newRange(range.from(), range.to());
            if (newRange != null) {
                ranges.addRange(newRange);
            }
        });
    }

    Backvocals getData() {
        List<BackRange> branges = new ArrayList<>();
        Measurer m = newMeasurer();
        for (Range range : ranges.getRanges()) {
            branges.add(new BackRange(m.frame2sec(range.from()), m.frame2sec(range.to())));
        }
        return new Backvocals(true, branges);
    }
}
