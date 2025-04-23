package karaed.engine.audio;

import karaed.engine.formats.ranges.Range;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.List;

public final class PreparedAudioSource {

    private final AudioSource source;
    private final AudioFormat format;
    private final int[] percents;

    private PreparedAudioSource(AudioSource source, AudioFormat format, int[] percents) {
        this.source = source;
        this.format = format;
        this.percents = percents;
    }

    public static PreparedAudioSource create(AudioSource source) throws IOException, UnsupportedAudioFileException {
        AudioFormat format;
        int channels;
        long[] maxValues;
        int frames;
        try (AudioInputStream as = source.getStream()) {
            WavReader reader = new WavReader(as);
            format = reader.format;
            channels = format.getChannels();
            maxValues = new long[channels];
            frames = reader.readAll((frame, values) -> {
                for (int i = 0; i < channels; i++) {
                    maxValues[i] = Math.max(maxValues[i], Math.abs(values[i]));
                }
            });
        }
        int[] percents = new int[frames];
        try (AudioInputStream as = source.getStream()) {
            WavReader reader = new WavReader(as);
            reader.readAll((frame, values) -> {
                float sum = 0;
                for (int i = 0; i < channels; i++) {
                    float percent = (float) Math.abs(values[i]) / maxValues[i] * 100f;
                    sum += percent;
                }
                percents[frame] = (int) (sum / channels);
            });
        }
        return new PreparedAudioSource(source, format, percents);
    }

    public int frames() {
        return percents.length;
    }

    public float frameRate() {
        return format.getFrameRate();
    }

    public Clip open(int from, int to) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        return source.open(from, to);
    }

    public List<Range> detectVoice(RangeParams params) {
        Ranger ranger = new Ranger(params);
        for (int i = 0; i < percents.length; i++) {
            int threshold = params.silenceThreshold(i);
            ranger.add(i, percents[i] <= threshold);
        }
        return ranger.finish(percents.length);
    }
}
