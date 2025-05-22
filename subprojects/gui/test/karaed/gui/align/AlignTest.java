package karaed.gui.align;

import java.nio.file.Path;

public final class AlignTest {

    public static void main(String[] args) throws Exception {
        ManualAlign ma = ManualAlign.create(
            null, Throwable::printStackTrace, false,
            Path.of("work\\align_test\\vocals.wav"),
            Path.of("work\\align_test\\text.txt"),
            Path.of("work\\align_test\\ranges.json"),
            Path.of("work\\align_test\\lang.json")
        );
        ma.setVisible(true);
    }
}
