package karaed.gui.save;

import karaed.model.Range;

import java.util.List;

public record SaveData(
    String vocalsPath,
    List<Range> ranges,
    List<String> origLines,
    List<String> editedLines
) {}
