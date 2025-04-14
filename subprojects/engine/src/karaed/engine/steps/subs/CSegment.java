package karaed.engine.steps.subs;

final class CSegment {

    final char ch;
    Timestamps timestamps;

    CSegment(char ch) {
        this.ch = ch;
    }

    @Override
    public String toString() {
        return String.valueOf(ch);
    }
}
