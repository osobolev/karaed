package karaed.engine.lyrics;

import karaed.engine.formats.info.Info;
import karaed.engine.formats.lrc.LRCResult;
import karaed.engine.opts.OInput;
import karaed.engine.steps.youtube.Youtube;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class LRCLib {

    private final HttpClient client;

    public LRCLib(HttpClient client) {
        this.client = client;
    }

    private <T> T get(Class<T> cls, String path, String... params) throws IOException, InterruptedException {
        List<String> query = new ArrayList<>();
        for (int i = 0; i < params.length; i += 2) {
            String name = params[i];
            String value = params[i + 1];
            if (value == null)
                continue;
            query.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        String url = "https://lrclib.net/api/" + path + "?" + String.join("&", query);
        String version = System.getProperty("jpackage.app-version");
        String userAgent = String.format(
            "karaed%s (https://github.com/osobolev/karaed)",
            version == null ? "" : "/" + version
        );
        HttpRequest request = HttpRequest
            .newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", userAgent)
            .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            return null;
        }
        try (InputStream is = response.body()) {
            return JsonUtil.parse(new InputStreamReader(is, StandardCharsets.UTF_8), cls);
        }
    }

    public LRCResult exact(Info info) throws IOException, InterruptedException {
        if (info.artist() == null || info.track() == null)
            return null;
        String duration;
        if (info.duration() == null) {
            duration = null;
        } else {
            duration = String.valueOf(Math.round(info.duration().floatValue()));
        }
        return get(
            LRCResult.class, "get",
            "artist_name", info.artist(),
            "album_name", info.album(),
            "track_name", info.track(),
            "duration", duration
        );
    }

    public LRCResult[] search(Info info) throws IOException, InterruptedException {
        String title = info.title() != null ? info.title() : info.fulltitle();
        if (info.track() == null && title == null)
            return null;
        return get(
            LRCResult[].class, "search",
            "artist_name", info.artist(),
            "album_name", info.album(),
            "track_name", info.track(),
            "q", title
        );
    }

    public static LRCResult loadLyrics(Info info) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            LRCLib lib = new LRCLib(client);
            LRCResult exact = lib.exact(info);
            if (exact != null)
                return exact;
            LRCResult[] found = lib.search(info);
            if (found == null || found.length <= 0)
                return null;
            return found[0];
        }
    }

    private static String fullTrack(Info info) {
        if (info.artist() != null && info.track() != null) {
            return "\"" + info.track() + "\" by " + info.artist();
        } else if (info.title() != null) {
            return "\"" + info.title() + "\"";
        } else if (info.fulltitle() != null) {
            return "\"" + info.fulltitle() + "\"";
        } else {
            return "";
        }
    }

    public static String loadLyrics(ToolRunner runner, OInput input) throws IOException, InterruptedException, LRCException {
        Info info = Youtube.metaInfo(runner, input);

        LRCResult lrc = loadLyrics(info);
        if (lrc == null) {
            throw new LRCException("LRCLib does not have the song " + fullTrack(info));
        }

        String lyrics = lrc.plainLyrics();
        if (lyrics == null) {
            throw new LRCException("LRCLib does not have lyrics for the song " + fullTrack(info));
        }

        return lyrics;
    }
}
