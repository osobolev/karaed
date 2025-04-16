package karaed.engine.audio;

import karaed.engine.KaraException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class WavReader {

    private final InputStream as;
    public final AudioFormat format;
    private final int bytes;
    private final int shift;
    private final long scale;
    private final byte[] buf;
    private int bufLen = 0;
    private int bufRead = 0;

    private int frame = 0;
    private final long[] values;

    public WavReader(AudioInputStream as) {
        this.as = as;
        this.format = as.getFormat();
        this.bytes = format.getSampleSizeInBits() / 8;
        this.shift = 64 - format.getSampleSizeInBits();
        this.scale = 1L << (format.getSampleSizeInBits() - 1);
        int frameSize = format.getFrameSize();
        this.buf = new byte[frameSize * 1024];
        this.values = new long[format.getChannels()];
    }

    private long decode(byte[] buf, int from) {
        long result = 0;
        if (format.isBigEndian()) {
            for (int i = 0; i < bytes; i++) {
                int b = buf[from + i] & 0xFF;
                result = (result << 8) | b;
            }
        } else {
            for (int i = 0; i < bytes; i++) {
                int b = buf[from + bytes - 1 - i] & 0xFF;
                result = (result << 8) | b;
            }
        }
        AudioFormat.Encoding encoding = format.getEncoding();
        if (encoding == AudioFormat.Encoding.PCM_SIGNED) {
            return (result << shift) >> shift;
        } else if (encoding == AudioFormat.Encoding.PCM_UNSIGNED) {
            return result - scale;
        }
        throw new KaraException("Unsupported WAV encoding: " + encoding);
    }

    private boolean fillBuffer() throws IOException {
        if (bufRead >= bufLen) {
            bufRead = 0;
            bufLen = as.readNBytes(buf, 0, buf.length);
            return bufLen > 0;
        } else {
            return true;
        }
    }

    public interface WavConsumer {

        void consume(int frame, long[] values);
    }

    public boolean read(WavConsumer consumer) throws IOException {
        if (!fillBuffer())
            return false;
        while (bufRead < bufLen) {
            for (int i = 0; i < format.getChannels(); i++) {
                values[i] = decode(buf, bufRead);
                bufRead += bytes;
            }
            consumer.consume(frame, values);
            frame++;
        }
        return true;
    }

    public int readAll(WavConsumer consumer) throws IOException {
        while (true) {
            if (!read(consumer))
                break;
        }
        return frame;
    }
}
