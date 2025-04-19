package karaed.engine.steps.subs;

final class TargetSegment {

    final String text;
    final boolean letters;
    Timestamps timestamps;

    TargetSegment(String text, boolean letters) {
        this.text = text;
        this.letters = letters;
    }

    @Override
    public String toString() {
        return text;
    }
}
