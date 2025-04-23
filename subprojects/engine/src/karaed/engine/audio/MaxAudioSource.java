package karaed.engine.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;

public final class MaxAudioSource {

    public final AudioSource source;
    public final AudioFormat format;
    public final int frames;
    public final long[] maxValues;

    public MaxAudioSource(AudioSource source, AudioFormat format, int frames, long[] maxValues) {
        this.source = source;
        this.format = format;
        this.frames = frames;
        this.maxValues = maxValues;
    }

    public static MaxAudioSource detectMaxValues(AudioSource source) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream as = source.getStream()) {
            WavReader reader = new WavReader(as);
            AudioFormat format = reader.format;
            long[] maxValues = new long[format.getChannels()];
            int frames = reader.readAll((frame, values) -> {
                for (int i = 0; i < maxValues.length; i++) {
                    maxValues[i] = Math.max(maxValues[i], Math.abs(values[i]));
                }
            });
            return new MaxAudioSource(source, format, frames, maxValues);
        }
    }
}
