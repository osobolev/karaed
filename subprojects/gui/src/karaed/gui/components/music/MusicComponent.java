package karaed.gui.components.music;

import karaed.engine.formats.ranges.Range;
import karaed.gui.components.ColorSequence;
import karaed.gui.components.model.EditableRanges;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.MenuBuilder;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.swing.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public abstract class MusicComponent extends JComponent implements Scrollable {

    private final BaseWindow owner;
    private final ColorSequence colors;
    protected final EditableRanges model;
    private IntFunction<String> getText;
    protected final float frameRate;

    private final List<Runnable> playChangeListeners = new ArrayList<>();
    private final List<IntConsumer> goToListeners = new ArrayList<>();

    protected float pixPerSec = 30.0f;

    private Range playingRange = null;
    private int playingY;
    private Clip playing = null;
    private long playingStarted;
    private final Timer playingTimer = new Timer(100, e -> {
        if (playingRange != null) {
            Sizer s = newSizer();
            int x1 = s.frame2x(playingRange.from());
            int x2 = s.frame2x(playingRange.to());
            repaint(x1 - 3, playingY - 1, Sizer.width(x1, x2) + 4, Sizer.SEEK_H + 2);
        }
    });

    private final RangeIndexes paintedRangeIndex = new RangeIndexes();

    protected MusicComponent(BaseWindow owner, ColorSequence colors, EditableRanges model) {
        this.owner = owner;
        this.colors = colors;
        this.model = model;
        this.frameRate = model.source.frameRate();

        playingTimer.setInitialDelay(0);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    protected final Measurer newMeasurer() {
        return new Measurer(frameRate, pixPerSec);
    }

    protected abstract Sizer newSizer();

    private int totalSeconds() {
        return (int) Math.ceil(model.source.frames() / frameRate);
    }

    protected final boolean rangeMouseClick(MouseEvent me, Sizer s, int frame,
                                            BiConsumer<MenuBuilder, Range> addRangeItems) {
        Range range = s.findRange(frame, me.getY(), model);
        if (range != null) {
            rangeClicked(me, range, s.seekY1(), menu -> addRangeItems.accept(menu, range));
            return true;
        }
        return false;
    }

    protected final void rangeClicked(MouseEvent me, Range range, int py,
                                      Consumer<MenuBuilder> addItems) {
        if (me.getButton() == MouseEvent.BUTTON1) {
            playRange(range, py);
        } else if (me.getButton() == MouseEvent.BUTTON3) {
            MenuBuilder menu = new MenuBuilder(me);
            menu.add("Play", () -> playRange(range, py));
            if (range == playingRange) {
                menu.add("Stop", this::stop);
            }
            Integer index = paintedRangeIndex.getIndex(range);
            if (index != null) {
                menu.add("Go to lyrics", () -> {
                    for (IntConsumer listener : goToListeners) {
                        listener.accept(index.intValue());
                    }
                });
            }
            addItems.accept(menu);
            menu.showMenu();
        }
    }

    public final void setTooltipSource(IntFunction<String> getText) {
        this.getText = getText;
    }

    @Override
    public final String getToolTipText(MouseEvent e) {
        if (getText == null)
            return null;
        Sizer s = newSizer();
        int frame = s.x2frame(e.getX());
        Range range = s.findRange(frame, e.getY(), model);
        Integer index = paintedRangeIndex.getIndex(range);
        if (index == null)
            return null;
        return getText.apply(index.intValue());
    }

    public final void showRange(int lineIndex, boolean play) {
        Range range = paintedRangeIndex.getRange(lineIndex);
        if (range == null)
            return;
        JViewport vp = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
        if (vp != null) {
            Measurer m = newMeasurer();
            int from = m.frame2x(range.from());
            int to = m.frame2x(range.to());
            int width = vp.getExtentSize().width;
            int x = Math.max((from + to - width) / 2, 0);
            vp.setViewPosition(new Point(x, 0));
        }
        if (play) {
            Sizer s = newSizer();
            playRange(range, s.seekY1());
        }
    }

    public final void addGoToListener(IntConsumer listener) {
        goToListeners.add(listener);
    }

    public final void setScale(float pixPerSec) {
        this.pixPerSec = pixPerSec;
        revalidate();
        repaint();
    }

    public final void recolor() {
        repaint();
    }

    // Playing:

    private void playRange(Range range, int y) {
        stop();
        try {
            Clip clip = model.source.open(range.from(), range.to());
            playingRange = range;
            playingY = y;
            playing = clip;
            clip.addLineListener(le -> {
                if (le.getType() == LineEvent.Type.STOP) {
                    stop();
                }
            });
            firePlayChanged();
            playingStarted = System.currentTimeMillis();
            playingTimer.start();
            clip.start();
            repaint();
        } catch (Exception ex) {
            owner.error(ex);
        }
    }

    public final boolean isPlaying() {
        return playing != null;
    }

    public final void stop() {
        if (playing != null) {
            playingTimer.stop();
            playing.stop();
            playingRange = null;
            playing = null;
            firePlayChanged();
            repaint();
        }
    }

    private void firePlayChanged() {
        for (Runnable listener : playChangeListeners) {
            listener.run();
        }
    }

    public final void addPlayChangeListener(Runnable listener) {
        playChangeListeners.add(listener);
    }

    // Painting:

    @Override
    protected final void paintComponent(Graphics g) {
        paintedRangeIndex.clear();

        super.paintComponent(g);

        int width = getWidth();
        int height = getHeight();
        Painter painter = doPaint(g, width, height);

        painter.paintScale(totalSeconds(), width);
        if (playingRange != null) {
            painter.paintPlay(playingRange, playingY, System.currentTimeMillis() - playingStarted);
        }
    }

    protected abstract Painter doPaint(Graphics g, int width, int height);

    protected final void paintRanges(Painter painter, boolean saveRangeIndexes) {
        painter.paintRanges(colors, model.getRanges(), saveRangeIndexes ? paintedRangeIndex : null);
    }

    // Scrolling:

    @Override
    public final Dimension getPreferredSize() {
        Sizer s = newSizer();
        return new Dimension(s.prefWidth(totalSeconds()), s.prefHeight());
    }

    @Override
    public final Dimension getPreferredScrollableViewportSize() {
        Sizer s = newSizer();
        return new Dimension(1000, s.prefHeight());
    }

    @Override
    public final int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 50;
    }

    @Override
    public final int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return 50;
        } else {
            return Math.max(visibleRect.width - 50, 50);
        }
    }

    @Override
    public final boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public final boolean getScrollableTracksViewportHeight() {
        return true;
    }
}
