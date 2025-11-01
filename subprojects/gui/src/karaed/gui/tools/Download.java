package karaed.gui.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;

final class Download {

    static void download(String url, ContentHandler handler) throws IOException {
        URLConnection conn = URI.create(url).toURL().openConnection();
        try (InputStream is = conn.getInputStream()) {
            handler.accept(is);
        }
        if (conn instanceof HttpURLConnection http) {
            http.disconnect();
        }
    }

    interface ContentHandler {

        void accept(InputStream is) throws IOException;
    }
}
