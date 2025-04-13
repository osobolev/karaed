package karaed.engine.opts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OCut(
    String from,
    String to
) {

    private static final Pattern PATTERN1 = Pattern.compile("((\\d+)h)?((\\d+)m)?((\\d+)s)?");
    private static final Pattern PATTERN2 = Pattern.compile("(((\\d+):)?(\\d+):)?(\\d+)");

    public OCut() {
        this(null, null);
    }

    private static int parseInt(String str) {
        if (str == null)
            return 0;
        return Integer.parseInt(str);
    }

    public static Double parseTime(String t) {
        String str = t.replaceAll("\\s+", "");
        Matcher m1 = PATTERN1.matcher(str);
        int hours;
        int minutes;
        int seconds;
        if (m1.matches()) {
            hours = parseInt(m1.group(2));
            minutes = parseInt(m1.group(4));
            seconds = parseInt(m1.group(6));
        } else {
            Matcher m2 = PATTERN2.matcher(str);
            if (m2.matches()) {
                hours = parseInt(m2.group(3));
                minutes = parseInt(m2.group(4));
                seconds = parseInt(m2.group(5));
            } else {
                return null;
            }
        }
        return (double) ((hours * 60 + minutes) * 60 + seconds);
    }
}
