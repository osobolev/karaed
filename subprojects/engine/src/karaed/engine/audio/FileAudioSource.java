package karaed.engine.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public final class FileAudioSource implements AudioSource {

    public final File file;

    public FileAudioSource(File file) {
        this.file = file;
    }

    @Override
    public AudioInputStream getStream() throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(file);
    }

    @Override
    public Clip open(int from, int to) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        AudioFormat format;
        byte[] data;
        int read;
        try (AudioInputStream as = AudioSystem.getAudioInputStream(file)) {
            format = as.getFormat();
            int frameSize = format.getFrameSize();
            as.skip((long) from * frameSize);
            data = new byte[(to - from) * frameSize];
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
        try (AudioInputStream as = AudioSystem.getAudioInputStream(file)) {
            format = as.getFormat();
            int frameSize = format.getFrameSize();
            as.skip((long) from * frameSize);
            data = new byte[(to - from) * frameSize];
            read = as.readNBytes(data, 0, data.length);
        }
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
        AudioInputStream piece = new AudioInputStream(new ByteArrayInputStream(data, 0, read), format, to - from);
        AudioSystem.write(piece, fileFormat.getType(), out);
    }
}
