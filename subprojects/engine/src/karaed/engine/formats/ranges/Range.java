package karaed.engine.formats.ranges;

import java.time.Duration;

public record Range(
    int from,
    int to
) implements RangeLike {

    public static String formatTime(float totalSeconds) {
        Duration duration = Duration.ofSeconds(Math.round(totalSeconds));
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();
        if (hours > 0) {
            return String.format("%s:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%s:%02d", minutes, seconds);
        }
    }

    private static String formatFrame(int frame) {
        float totalSeconds = frame / 44_100f;
        return formatTime(totalSeconds);
    }

    public static int mid(int a, int b) {
        return a + (b - a) / 2;
    }

    @Override
    public String toString() {
        return String.format("%d - %d (%s - %s)", from, to, formatFrame(from), formatFrame(to));
    }
}
