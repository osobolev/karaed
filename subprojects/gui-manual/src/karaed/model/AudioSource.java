package karaed.model;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.OutputStream;

public interface AudioSource {

    AudioInputStream getStream() throws UnsupportedAudioFileException, IOException;

    Clip open(int from, int to) throws UnsupportedAudioFileException, IOException, LineUnavailableException;

    void cut(int from, int to, OutputStream out) throws UnsupportedAudioFileException, IOException;
}
