package karaed.engine.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public final class FileAudioSource implements AudioSource {

    private final File file;

    public FileAudioSource(File file) {
        this.file = file;
    }

    @Override
    public AudioFormat getFormat() throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioFileFormat(file).getFormat();
    }

    private static int toBytes(AudioFormat format, int frames) {
        return frames * format.getFrameSize();
    }

    @Override
    public AudioInputStream getStream() throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(file);
    }

    private AudioInputStream subStream(int from) throws UnsupportedAudioFileException, IOException {
        AudioInputStream as = getStream();
        if (from > 0) {
            as.skip(toBytes(as.getFormat(), from));
        }
        return as;
    }

    private static byte[] pieceBuf(AudioFormat format, int from, int to) {
        return new byte[toBytes(format, to - from)];
    }

    @Override
    public Clip open(int from, int to) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioFormat format;
        byte[] data;
        int read;
        try (AudioInputStream as = subStream(from)) {
            format = as.getFormat();
            data = pieceBuf(format, from, to);
            read = as.readNBytes(data, 0, data.length);
        }
        Clip clip = AudioSystem.getClip();
        clip.open(format, data, 0, read);
        return clip;
    }

    @Override
    public void cut(int from, int to, OutputStream out) throws UnsupportedAudioFileException, IOException {
        AudioFormat format;
        byte[] data;
        int read;
        try (AudioInputStream as = subStream(from)) {
            format = as.getFormat();
            data = pieceBuf(format, from, to);
            read = as.readNBytes(data, 0, data.length);
        }
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream piece = new AudioInputStream(new ByteArrayInputStream(data, 0, read), format, to - from);
        AudioSystem.write(piece, fileFormat.getType(), out);
    }
}
