package karaed.manual.gui.save;

import karaed.manual.model.Range;

import java.util.List;

public record SaveData(
    String vocalsPath,
    List<Range> ranges,
    List<String> origLines,
    List<String> editedLines
) {}
