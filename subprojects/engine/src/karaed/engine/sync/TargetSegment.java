package karaed.engine.sync;

public final class TargetSegment {

    public final String text;
    public final boolean letters;
    Timestamps timestamps;

    TargetSegment(String text, boolean letters) {
        this.text = text;
        this.letters = letters;
    }

    public Timestamps timestamps() {
        return timestamps;
    }

    @Override
    public String toString() {
        return text;
    }
}
