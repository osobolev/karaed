package karaed.engine.formats.info;

import java.util.ArrayList;
import java.util.List;

public record Info(
    String artist,
    String album,
    String track,
    String title,
    String fulltitle,
    Double duration,
    String ext
) {

    public String shortTitle() {
        return title != null ? title : fulltitle;
    }

    public String longTitle() {
        return fulltitle != null ? fulltitle : title;
    }

    public List<String> getTitles() {
        List<String> titles = new ArrayList<>();
        if (track != null) {
            titles.add(track);
            if (artist != null) {
                titles.add("by " + artist);
            }
        } else if (longTitle() != null) {
            titles.add(longTitle());
        }
        return titles;
    }

    @Override
    public String toString() {
        if (track != null && artist != null) {
            return artist + " - " + track;
        } else if (longTitle() != null) {
            return longTitle();
        } else {
            return "-";
        }
    }
}
