package karaed.engine.audio;

import karaed.engine.formats.ranges.Range;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.List;

public final class VoiceRanges {

    private static boolean isSilence(long[] values, long[] maxValues, float threshold) {
        for (int i = 0; i < values.length; i++) {
            float value = (float) values[i] / maxValues[i];
            if (value > threshold)
                return false;
        }
        return true;
    }

    private static WavReader.WavConsumer consumer(Ranger ranger, long[] maxValues, RangeParams params) {
        return (frame, values) -> ranger.add(frame, isSilence(values, maxValues, params.silenceThreshold(frame)));
    }

    public static List<Range> detectVoice(MaxAudioSource source, RangeParams params) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream as = source.source.getStream(0)) {
            WavReader reader = new WavReader(as, 0);
            Ranger ranger = new Ranger(params);
            int frames = reader.readAll(consumer(ranger, source.maxValues, params));
            return ranger.finish(frames);
        }
    }

//    public static List<Range> resplit(MaxAudioSource source, Range range,
//                                      RangeParams params) throws IOException, UnsupportedAudioFileException {
//        try (AudioInputStream as = source.source.getStream(range.from())) {
//            WavReader reader = new WavReader(as, range.from());
//            Ranger ranger = ranger(params);
//            WavReader.WavConsumer wavConsumer = consumer(ranger, source.maxValues, params.silenceThreshold());
//            int frames = range.to() - range.from();
//            reader.readN(wavConsumer, frames);
//            return ranger.finish(range.to());
//        }
//    }
}
