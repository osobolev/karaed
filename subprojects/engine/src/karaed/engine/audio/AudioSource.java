package karaed.engine.audio;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.OutputStream;

public interface AudioSource {

    AudioFormat getFormat() throws UnsupportedAudioFileException, IOException;

    AudioInputStream getStream(int from) throws UnsupportedAudioFileException, IOException;

    Clip open(int from, int to) throws UnsupportedAudioFileException, IOException, LineUnavailableException;

    void cut(int from, int to, OutputStream out) throws UnsupportedAudioFileException, IOException;
}
