package karaed.gui.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

final class Download {

    static void download(String url, ContentHandler handler) throws IOException, InterruptedException {
        URLConnection conn = URI.create(url).toURL().openConnection();
        try {
            try (InputStream is = conn.getInputStream()) {
                handler.accept(is);
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
