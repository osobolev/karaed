package karaed.gui.components;

import karaed.gui.components.lyrics.LyricsComponent;
import karaed.gui.components.model.EditableRanges;
import karaed.gui.components.music.MusicComponent;
import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Function;

public final class MusicAndLyrics<M extends MusicComponent> {

    private static final Icon ICON_STOP = InputUtil.getIcon("/stop.png");

    private final ColorSequence colors = new ColorSequence();
    private final EditableRanges model;

    public final M music;
    public final LyricsComponent lyrics = new LyricsComponent(colors);
    public final JSlider scaleSlider = new JSlider(2, 50, 30);
    public final Action actionStop;

    public MusicAndLyrics(EditableRanges model, List<String> lines, boolean editable,
                          Function<ColorSequence, M> musicFactory) {
        this.model = model;
        this.music = musicFactory.apply(colors);

        lyrics.setEditable(editable);
        lyrics.addLyricsListener(music::showRange);
        music.addGoToListener(lyrics::goTo);
        music.setTooltipSource(lyrics::getLineAt);

        this.actionStop = new AbstractAction("Stop", ICON_STOP) {
            @Override
            public void actionPerformed(ActionEvent e) {
                music.stop();
            }
        };

        setScale();
        scaleSlider.addChangeListener(e -> setScale());

        enableDisableStop();
        music.addPlayChangeListener(this::enableDisableStop);

        lyrics.setLines(lines);

        recolor(true, true);
    }

    public void recolor(boolean music, boolean lyrics) {
        syncNumbers();
        if (music) {
            this.music.recolor();
        }
        if (lyrics) {
            this.lyrics.recolor();
        }
    }

    private void syncNumbers() {
        int n = Math.min(model.getRangeCount(), lyrics.getLineCount());
        colors.setNumber(n);
    }

    private void setScale() {
        music.setScale(scaleSlider.getValue());
    }

    private void enableDisableStop() {
        actionStop.setEnabled(music.isPlaying());
    }

    public JScrollPane wrapMusic() {
        return new JScrollPane(music, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }
}
