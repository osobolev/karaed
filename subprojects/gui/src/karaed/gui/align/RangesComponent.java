package karaed.gui.align;

import karaed.engine.audio.MaxAudioSource;
import karaed.engine.audio.VoiceRanges;
import karaed.engine.formats.ranges.Range;
import karaed.gui.ErrorLogger;
import karaed.gui.util.ShowMessage;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
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

    private Range playingRange = null;
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
            int x = LPAD + from;
            int y = h + 20;
            int width = Math.max(to - from, 1);
            int height = 20;
            g.fillRect(x, y, width, height);
            if (range == playingRange) {
                g.setColor(Color.red);
                g.drawRect(x - 1, y - 1, width + 1, height + 1);
            }
        }
    }

    private void mouseClick(MouseEvent me) {
        int x = me.getX() - LPAD;
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
        if (me.getButton() == MouseEvent.BUTTON1) {
            stop();
            try {
                Clip clip = source.source.open(range.from(), range.to());
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
            JPopupMenu menu = new JPopupMenu();
            int index = irange;
            menu.add(new AbstractAction("Resplit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    resplit(index);
                }
            });
            menu.show(this, me.getX() - 5, me.getY() - 5);
        }
    }

    private void resplit(int index) {
        Range range = ranges.get(index);
        List<Range> newRanges;
        try {
            float ignoreShortSilence = 0.25f; // todo: ask for new value
            newRanges = VoiceRanges.resplit(source, range, ignoreShortSilence);
        } catch (Exception ex) {
            ShowMessage.error(this, logger, ex);
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
