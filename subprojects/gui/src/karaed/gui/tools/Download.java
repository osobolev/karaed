package karaed.gui.tools;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.function.IntConsumer;

final class Download {

    private static final class ProgressInputStream extends FilterInputStream {

        private final long size;
        private final IntConsumer progress;

        private long totalRead = 0;
        private int lastPercent = -1;

        ProgressInputStream(InputStream in, long size, IntConsumer progress) {
            super(in);
            this.size = size;
            this.progress = progress;
        }

        private void progress(long read) {
            if (read <= 0)
                return;
            totalRead += read;
            int percent = Math.round((float) totalRead / size * 100f);
            if (percent != lastPercent) {
                progress.accept(percent);
                lastPercent = percent;
            }
        }

        @Override
        public int read() throws IOException {
            int c = in.read();
            if (c >= 0) {
                progress(1);
            }
            return c;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = in.read(b);
            progress(read);
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = in.read(b, off, len);
            progress(read);
            return read;
        }

        @Override
        public long skip(long n) throws IOException {
            long skip = in.skip(n);
            progress(skip);
            return skip;
        }
    }

    static void download(String url, IntConsumer progress, ContentHandler handler) throws IOException, InterruptedException {
        URLConnection conn = URI.create(url).toURL().openConnection();
        try {
            try (InputStream is = conn.getInputStream()) {
                InputStream src;
                if (progress != null) {
                    long contentLength;
                    contentLength = conn.getHeaderFieldLong("Content-Length", -1L);
                    if (contentLength > 0) {
                        src = new ProgressInputStream(is, contentLength, progress);
                    } else {
                        src = is;
                    }
                } else {
                    src = is;
                }
                handler.accept(src);
            }
        } finally {
            if (conn instanceof HttpURLConnection http) {
                http.disconnect();
            }
        }
    }

    interface ContentHandler {

        void accept(InputStream is) throws IOException, InterruptedException;
    }
}
