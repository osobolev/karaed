package karaed.engine.audio;

import javax.sound.sampled.*;
import java.io.*;

public final class MemAudioSource implements AudioSource {

    private final AudioFileFormat format;
    private final byte[] data;

    public MemAudioSource(AudioFileFormat format, byte[] data) {
        this.format = format;
        this.data = data;
    }

    public static MemAudioSource create(File file) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat format = AudioSystem.getAudioFileFormat(file);
        try (AudioInputStream as = AudioSystem.getAudioInputStream(file)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            as.transferTo(bos);
            return new MemAudioSource(format, bos.toByteArray());
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format.getFormat();
    }

    private int getFrameSize() {
        return format.getFormat().getFrameSize();
    }

    private AudioInputStream subStream(int from, int to) {
        int frameSize = getFrameSize();
        return new AudioInputStream(
            new ByteArrayInputStream(data, from * frameSize, (to - from) * frameSize),
            format.getFormat(), to - from
        );
    }

    @Override
    public AudioInputStream getStream() {
        int frames = data.length / getFrameSize();
        return subStream(0, frames);
    }

    @Override
    public Clip open(int from, int to) throws LineUnavailableException {
        int frameSize = getFrameSize();
        Clip clip = AudioSystem.getClip();
        clip.open(format.getFormat(), data, from * frameSize, (to - from) * frameSize);
        return clip;
    }

    @Override
    public void cut(int from, int to, OutputStream out) throws IOException {
        AudioInputStream piece = subStream(from, to);
        AudioSystem.write(piece, format.getType(), out);
    }
}
