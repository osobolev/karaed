package karaed.gui.align;

import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Range;

import java.util.List;

record SavedData(
    AreaParams params,
    List<Range> ranges
) {}
