package karaed.tools;

import java.io.IOException;
import java.io.Reader;

final class CaptureThread<T> {

    private final Thread thread;
    private T result = null;

    private CaptureThread(OutputProcessor<T> processor, Reader stream) {
        this.thread = new Thread(() -> {
            try {
                result = processor.process(stream);
            } catch (IOException ex) {
                // ignore
            }
        });
    }

    static <T> CaptureThread<T> start(OutputProcessor<T> processor, Reader stream) {
        CaptureThread<T> ct = new CaptureThread<>(processor, stream);
        ct.thread.start();
        return ct;
    }

    T join() throws InterruptedException {
        thread.join();
        return result;
    }
}
