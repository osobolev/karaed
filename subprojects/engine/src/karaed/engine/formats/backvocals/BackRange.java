package karaed.engine.formats.backvocals;

public record BackRange(
    double from,
    double to,
    Double coeff
) {

    public double coefficient() {
        return coeff == null ? 1.0 : coeff.doubleValue();
    }
}
