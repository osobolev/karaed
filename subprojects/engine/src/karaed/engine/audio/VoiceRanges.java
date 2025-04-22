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

    private static WavReader.WavConsumer consumer(Ranger ranger, long[] maxValues, float threshold) {
        return (frame, values) -> ranger.add(frame, isSilence(values, maxValues, threshold));
    }

    public static List<Range> detectVoice(MaxAudioSource source, float silenceThreshold) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream as = source.source.getStream()) {
            WavReader reader = new WavReader(as, 0);
            float frameRate = source.format.getFrameRate();
            // todo: constant can change:
            Ranger ranger = new Ranger((int) (0.5f * frameRate));
            int frames = reader.readAll(consumer(ranger, source.maxValues, silenceThreshold));
            return ranger.finish(frames);
        }
    }

    public static List<Range> resplit(MaxAudioSource source, Range range,
                                      float silenceThreshold, float ignoreShortSilence) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream as = source.source.getStream()) {
            WavReader reader = new WavReader(as, range.from());
            float frameRate = reader.format.getFrameRate();
            Ranger ranger = new Ranger((int) (ignoreShortSilence * frameRate));
            WavReader.WavConsumer wavConsumer = consumer(ranger, source.maxValues, silenceThreshold);
            as.skip((long) range.from() * reader.format.getFrameSize());
            int frames = range.to() - range.from();
            reader.readN(wavConsumer, frames);
            return ranger.finish(range.to());
        }
    }
}
