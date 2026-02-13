package karaed.engine.formats.backvocals;

import java.util.Collections;
import java.util.List;

public record Backvocals(
    boolean manual,
    List<BackRange> ranges
) {

    public static final Backvocals EMPTY = new Backvocals(false, Collections.emptyList());
}
