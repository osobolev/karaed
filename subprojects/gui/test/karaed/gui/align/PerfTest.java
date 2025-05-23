package karaed.gui.align;

import karaed.engine.audio.PreparedAudioSource;
import karaed.engine.formats.ranges.AreaParams;
import karaed.gui.align.model.EditableRanges;

import java.io.File;
import java.util.Collections;

public final class PerfTest {

    public static void main(String[] args) throws Exception {
        PreparedAudioSource source = PreparedAudioSource.create(new File("work\\align_test\\vocals.wav"));
        AreaParams params = new AreaParams(35, 0.5f, 0.5f);
        EditableRanges model = new EditableRanges(source, params, Collections.emptyList(), Collections.emptyList());
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            model.splitByParams(null, params);
        }
        long t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
    }
}
