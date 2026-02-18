package karaed.gui.components.music;

import karaed.engine.audio.AudioSource;

public class Measurer {

    private static final int LPAD = 10;
    private static final int RPAD = 10;

    private final float frameRate;
    private final float pixPerSec;

    Measurer(float frameRate, float pixPerSec) {
        this.frameRate = frameRate;
        this.pixPerSec = pixPerSec;
    }

    final int sec2pix(float seconds) {
        return Math.round(seconds * pixPerSec);
    }

    final int sec2x(float seconds) {
        return LPAD + sec2pix(seconds);
    }

    public final float frame2sec(int frame) {
        return AudioSource.frame2sec(frame, frameRate);
    }

    public final int frame2x(int frame) {
        float seconds = frame2sec(frame);
        return sec2x(seconds);
    }

    public final int sec2frame(float seconds) {
        return AudioSource.sec2frame(seconds, frameRate);
    }

    public final int pix2frame(int pixels) {
        float seconds = pixels / pixPerSec;
        return sec2frame(seconds);
    }

    public final int x2frame(int x) {
        return pix2frame(x - LPAD);
    }

    final int prefWidth(int seconds) {
        int pixels = (int) Math.ceil(seconds * pixPerSec);
        return LPAD + pixels + RPAD;
    }

    public static int width(int x1, int x2) {
        return Math.max(x2 - x1, 1);
    }
}
