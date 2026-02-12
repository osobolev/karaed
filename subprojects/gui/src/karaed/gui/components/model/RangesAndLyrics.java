package karaed.gui.components.model;

import karaed.engine.audio.PreparedAudioSource;
import karaed.engine.formats.ranges.AreaParams;
import karaed.engine.formats.ranges.Ranges;
import karaed.json.JsonUtil;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public record RangesAndLyrics(
    EditableRanges ranges,
    List<String> rangeLines,
    boolean fromFile
) {

    public static Ranges loadData(Path rangesFile) throws IOException {
        if (Files.exists(rangesFile)) {
            return JsonUtil.readFile(rangesFile, Ranges.class);
        }
        return null;
    }

    public static RangesAndLyrics load(Path vocals, Path rangesFile, List<String> textLines) throws IOException, UnsupportedAudioFileException {
        PreparedAudioSource maxSource = PreparedAudioSource.create(vocals.toFile());

        Ranges fileData = loadData(rangesFile);

        EditableRanges model;
        List<String> rangeLines;
        if (fileData != null) {
            model = new EditableRanges(maxSource, fileData.params(), fileData.ranges(), fileData.areas());
            rangeLines = fileData.lines();
        } else {
            AreaParams params = new AreaParams(1, 0.5f, 0.5f);
            model = new EditableRanges(maxSource, params, Collections.emptyList(), Collections.emptyList());
            model.splitByParams(null, params);
            rangeLines = textLines;
        }

        return new RangesAndLyrics(model, rangeLines, fileData != null);
    }
}
