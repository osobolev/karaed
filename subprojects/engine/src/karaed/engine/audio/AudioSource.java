package karaed.engine.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public interface AudioSource {

    AudioFormat getFormat() throws UnsupportedAudioFileException, IOException;

    AudioInputStream getStream() throws UnsupportedAudioFileException, IOException;

    Clip open(int from, int to) throws UnsupportedAudioFileException, IOException, LineUnavailableException;

    void cut(int from, int to, OutputStream out) throws UnsupportedAudioFileException, IOException;

    static AudioSource create(File file) {
        return new FileAudioSource(file);
    }

    static int sec2frame(float seconds, float frameRate) {
        return Math.round(seconds * frameRate);
    }

    static float frame2sec(int frame, float frameRate) {
        return frame / frameRate;
    }
}
