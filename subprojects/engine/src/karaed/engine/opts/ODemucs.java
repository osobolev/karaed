package karaed.engine.opts;

public record ODemucs(
    int shifts
) {

    public ODemucs() {
        this(1);
    }
}
