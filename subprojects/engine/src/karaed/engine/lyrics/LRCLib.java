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
import java.util.function.Consumer;

public final class LRCLib {

    public static LRCResult loadLyrics(String artist, String track) throws IOException, InterruptedException {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            String url = String.format(
                "https://lrclib.net/api/get?artist_name=%s&track_name=%s",
                URLEncoder.encode(artist, StandardCharsets.UTF_8),
                URLEncoder.encode(track, StandardCharsets.UTF_8)
            );
            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "https://github.com/osobolev/karaed")
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

    public static String loadLyrics(ToolRunner runner, OInput input, Consumer<String> onSuccess) throws IOException, InterruptedException {
        Info info = Youtube.metaInfo(runner, input);

        String artist = info.artist();
        String track = info.track() != null ? info.track() : info.title();
        if (artist == null || track == null) {
            return "No artist/track information";
        }

        LRCResult lrc = loadLyrics(artist, track);
        if (lrc == null) {
            return "LRCLib does not have this song";
        }

        String lyrics = lrc.plainLyrics();
        if (lyrics == null) {
            return "LRCLib does not have lyrics for this song";
        }

        onSuccess.accept(lyrics);
        return null;
    }
}
