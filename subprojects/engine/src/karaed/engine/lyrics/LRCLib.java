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

public final class LRCLib {

    public static LRCResult loadLyrics(String artist, String track) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            String url = String.format(
                "https://lrclib.net/api/get?artist_name=%s&track_name=%s",
                URLEncoder.encode(artist, StandardCharsets.UTF_8),
                URLEncoder.encode(track, StandardCharsets.UTF_8)
            );
            String version = System.getProperty("jpackage.app-version");
            String userAgent = String.format(
                "karaed%s (+https://github.com/osobolev/karaed)",
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
                return JsonUtil.parse(new InputStreamReader(is, StandardCharsets.UTF_8), LRCResult.class);
            }
        }
    }

    private static String fullTrack(String artist, String track) {
        return "'" + track + "' by " + artist;
    }

    public static String loadLyrics(ToolRunner runner, OInput input) throws IOException, InterruptedException, LRCException {
        Info info = Youtube.metaInfo(runner, input);

        String artist = info.artist();
        String track = info.track() != null ? info.track() : info.title();
        if (artist == null || track == null) {
            throw new LRCException("No artist/track information");
        }

        LRCResult lrc = loadLyrics(artist, track);
        if (lrc == null) {
            throw new LRCException("LRCLib does not have the song " + fullTrack(artist, track));
        }

        String lyrics = lrc.plainLyrics();
        if (lyrics == null) {
            throw new LRCException("LRCLib does not have lyrics for the song " + fullTrack(artist, track));
        }

        return lyrics;
    }
}
