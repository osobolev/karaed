package karaed.manual;

import karaed.manual.model.MaxAudioSource;
import karaed.manual.model.Range;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.List;

public final class VoiceRanges {

    private static boolean isSilence(long[] values, long[] maxValues) {
        for (int i = 0; i < values.length; i++) {
            float value = (float) values[i] / maxValues[i];
            if (value > 0.01) // todo: threshold
                return false;
        }
        return true;
    }

    private static WavReader.WavConsumer consumer(Ranger ranger, long[] maxValues) {
        return (frame, values) -> ranger.add(frame, isSilence(values, maxValues));
    }

    public static List<Range> detectVoice(MaxAudioSource source) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream as = source.source.getStream()) {
            WavReader reader = new WavReader(as);
            float frameRate = source.format.getFrameRate();
            // todo: constant can change:
            Ranger ranger = new Ranger((int) (0.5f * frameRate));
            int frames = reader.readAll(consumer(ranger, source.maxValues));
            return ranger.finish(frames);
        }
    }

    public static List<Range> resplit(MaxAudioSource source, Range range, float ignoreShortSilence) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream as = source.source.getStream()) {
            WavReader reader = new WavReader(as);
            float frameRate = reader.format.getFrameRate();
            Ranger ranger = new Ranger((int) (ignoreShortSilence * frameRate));
            WavReader.WavConsumer wavConsumer = consumer(ranger, source.maxValues);
            as.skip((long) range.from() * reader.format.getFrameSize());
            int frames = range.to() - range.from();
            for (int i = 0; i < frames; i++) {
                reader.read(wavConsumer);
            }
            return ranger.finish(range.to());
        }
    }
}
