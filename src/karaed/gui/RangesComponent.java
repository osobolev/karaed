package karaed.gui;

import karaed.ErrorLogger;
import karaed.VoiceRanges;
import karaed.model.MaxAudioSource;
import karaed.model.Range;

import javax.sound.sampled.Clip;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

final class RangesComponent extends JComponent implements Scrollable{

    private static final int[] SECOND_TICKS = {1, 5, 10, 30, 60};
    private static final int LPAD = 10;
    private static final int RPAD = 10;

    private final ErrorLogger logger;
    private final ColorSequence colors;

    private MaxAudioSource source = null;
    private float frameRate = 0;
    private final List<Range> ranges = new ArrayList<>();

    private final List<Runnable> playChanged = new ArrayList<>();
    private final List<Runnable> rangesChanged = new ArrayList<>();

    private float pixPerSec = 30.0f;

    private Clip playing = null;

    RangesComponent(ErrorLogger logger, ColorSequence colors) {
        this.logger = logger;
        this.colors = colors;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mouseClick(e);
            }
        });
    }

    void setData(MaxAudioSource source, List<Range> ranges) {
        stop();

        this.source = source;
        this.frameRate = source.format.getFrameRate();
        this.ranges.clear();
        this.ranges.addAll(ranges);

        revalidate();
        repaint();
    }

    private int totalSeconds() {
        if (source == null)
            return 0;
        return (int) Math.ceil(source.frames / frameRate);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (ranges.isEmpty())
            return;

        FontMetrics fm = getFontMetrics(getFont());
        int minTickWidth = fm.stringWidth("00:00") * 4;
        int tick = 0;
        for (int tickSize : SECOND_TICKS) {
            int pixels = Math.round(tickSize * pixPerSec);
            if (pixels >= minTickWidth) {
                tick = tickSize;
                break;
            }
        }
        int h = fm.getHeight();
        if (tick > 0) {
            int seconds = totalSeconds();
            g.setColor(Color.red);
            for (int s = 0; s <= seconds; s += tick) {
                int x = LPAD + Math.round(s * pixPerSec);
                String str = Range.formatTime(s); // todo: for non-minute ticks paint only seconds???
                g.drawString(str, x - fm.stringWidth(str) / 2, fm.getAscent());
                g.drawLine(x, h, x, h + 10);
            }
        }

        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            Color color = colors.getColor(i);
            g.setColor(color == null ? Color.black : color);
            float secFrom = range.from() / frameRate;
            float secTo = range.to() / frameRate;
            int from = Math.round(secFrom * pixPerSec);
            int to = Math.round(secTo * pixPerSec);
            g.fillRect(LPAD + from, h + 20, to - from, 20);
        }
    }

    private void mouseClick(MouseEvent e) {
        int x = e.getX() - LPAD;
        float second = x / pixPerSec;
        int frame = Math.round(second * frameRate);
        int irange = -1;
        for (int i = 0; i < ranges.size(); i++) {
            Range range = ranges.get(i);
            if (frame >= range.from() && frame < range.to()) {
                irange = i;
                break;
            }
        }
        if (irange < 0)
            return;
        Range range = ranges.get(irange);
        if (e.getButton() == MouseEvent.BUTTON1) {
            stop();
            try {
                Clip clip = source.source.open(range.from(), range.to());
                playing = clip;
                firePlayChanged();
                clip.start();
            } catch (Exception ex) {
                logger.error(ex);
                ShowMessage.error(this, ex);
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menu = new JPopupMenu();
            int index = irange;
            menu.add(new AbstractAction("Resplit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    resplit(index);
                }
            });
            menu.show(this, e.getX() - 5, e.getY() - 5);
        }
    }

    private void resplit(int index) {
        Range range = ranges.get(index);
        List<Range> newRanges;
        try {
            float ignoreShortSilence = 0.25f; // todo: ask for new value
            newRanges = VoiceRanges.resplit(source, range, ignoreShortSilence);
        } catch (Exception ex) {
            logger.error(ex);
            ShowMessage.error(this, ex);
            return;
        }
        ranges.remove(index);
        ranges.addAll(index, newRanges);
        revalidate();
        repaint();
        fireRangesChanged();
    }

    @Override
    public Dimension getPreferredSize() {
        int seconds = totalSeconds();
        int pixels = (int) Math.ceil(seconds * pixPerSec);
        return new Dimension(LPAD + pixels + RPAD, 100); // todo: height
    }

    private void firePlayChanged() {
        for (Runnable runnable : playChanged) {
            runnable.run();
        }
    }

    boolean isPlaying() {
        return playing != null;
    }

    void stop() {
        if (playing != null) {
            playing.stop();
            playing = null;
            firePlayChanged();
        }
    }

    void setScale(float pixPerSec) {
        this.pixPerSec = pixPerSec;
        revalidate();
        repaint();
    }

    private void fireRangesChanged() {
        for (Runnable runnable : rangesChanged) {
            runnable.run();
        }
    }

    int getRangeCount() {
        return ranges.size();
    }

    List<Range> getRanges() {
        return ranges;
    }

    void recolor() {
        repaint();
    }

    void addPlayChanged(Runnable listener) {
        playChanged.add(listener);
    }

    void addRangesChanged(Runnable listener) {
        rangesChanged.add(listener);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(1000, 100); // todo
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
