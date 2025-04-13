package karaed.engine.formats.info;

import java.util.ArrayList;
import java.util.List;

public record Info(
    String artist,
    String track,
    String title,
    String fulltitle,
    String ext
) {

    public List<String> getTitles() {
        List<String> titles = new ArrayList<>();
        if (track != null) {
            titles.add(track);
            if (artist != null) {
                titles.add("by " + artist);
            }
        } else if (fulltitle != null) {
            titles.add(fulltitle);
        } else if (title != null) {
            titles.add(title);
        }
        return titles;
    }

    @Override
    public String toString() {
        if (track != null && artist != null) {
            return artist + " - " + track;
        } else if (fulltitle != null) {
            return fulltitle;
        } else {
            return title;
        }
    }
}
