package karaed.engine.formats.ranges;

public interface RangeLike {

    int from();

    int to();

    default boolean contains(int frame) {
        return from() <= frame && frame < to();
    }
}
