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

    private int toBytes(int frame) {
        return frame * getFrameSize();
    }

    private AudioInputStream subStream(int from, int to) {
        return new AudioInputStream(
            new ByteArrayInputStream(data, toBytes(from), toBytes(to - from)),
            format.getFormat(), to - from
        );
    }

    @Override
    public AudioInputStream getStream(int from) {
        int frames = data.length / getFrameSize();
        return subStream(from, frames - from);
    }

    @Override
    public Clip open(int from, int to) throws LineUnavailableException {
        Clip clip = AudioSystem.getClip();
        clip.open(format.getFormat(), data, toBytes(from), toBytes(to - from));
        return clip;
    }

    @Override
    public void cut(int from, int to, OutputStream out) throws IOException {
        AudioInputStream piece = subStream(from, to);
        AudioSystem.write(piece, format.getType(), out);
    }
}
