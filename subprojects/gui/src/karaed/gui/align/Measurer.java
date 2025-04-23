package karaed.gui.align;

class Measurer {

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

    final int frame2x(int frame) {
        float seconds = frame / frameRate;
        return sec2x(seconds);
    }

    final int x2frame(int x) {
        float seconds = (x - LPAD) / pixPerSec;
        return Math.round(seconds * frameRate);
    }

    final int prefWidth(int seconds) {
        int pixels = (int) Math.ceil(seconds * pixPerSec);
        return LPAD + pixels + RPAD;
    }
}
