package karaed.engine.steps.video;

import karaed.engine.formats.backvocals.Backvocals;
import karaed.engine.formats.ffprobe.FFStream;
import karaed.engine.formats.ffprobe.FFStreams;
import karaed.engine.video.VideoFinder;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MakeVideo {

    public static final String PREPARED = "prepared.";

    private static void addCodecs(Path video, List<String> args) {
        if (video != null && video.getFileName().toString().endsWith(".webm"))
            return;
        args.addAll(List.of(
            "-c:v", "libx264",
            "-c:a", "aac",
            "-b:a", "192k"
        ));
    }

    private static void enlargeVideo(ToolRunner runner, Path video, Path largeVideo, int origWidth, int origHeight) throws IOException, InterruptedException {
        ScalePad scale = ScalePad.create(origWidth, origHeight);
        runner.println(String.format("Enlarging small video to %s...", scale));
        List<String> ffmpeg = new ArrayList<>(List.of(
            "-i", video.toString(),
            "-y", "-stats",
            "-vf", scale.getFormat()
        ));
        addCodecs(video, ffmpeg);
        ffmpeg.add(largeVideo.toString());
        runner.run().ffmpeg(ffmpeg);
    }

    public static void doPrepareVideo(ToolRunner runner, Path video, Path preparedVideo) throws IOException, InterruptedException {
        FFStreams streams = runner.run(JsonUtil.parser(FFStreams.class)).ffprobe(
            "-print_format", "json",
            "-select_streams", "v:0",
            "-show_entries", "stream=width,height",
            video.toString()
        );
        FFStream stream = streams.streams().getFirst();
        int width = stream.width();
        int height = stream.height();
        if (width < 1280) {
            enlargeVideo(runner, video, preparedVideo, width, height);
        } else {
            Files.write(preparedVideo, new byte[0]);
        }
    }

    public static void prepareVideo(ToolRunner runner, VideoFinder finder) throws IOException, InterruptedException {
        Path video = finder.getVideo("", false);
        if (video == null || !Files.exists(video))
            return;
        Path preparedVideo = finder.getVideo(PREPARED, true);
        doPrepareVideo(runner, video, preparedVideo);
    }

    private static String escapeFilter(String path) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '\\') {
                buf.append('/');
            } else if (ch == ':') {
                buf.append("\\\\:");
            } else {
                buf.append(ch);
            }
        }
        return buf.toString();
    }

    public static Path getVideo(VideoFinder finder) throws IOException {
        Path preparedVideo = finder.getVideo(PREPARED, false);
        if (preparedVideo != null && Files.exists(preparedVideo) && Files.size(preparedVideo) > 0)
            return preparedVideo;
        Path originalVideo = finder.getVideo("", false);
        if (originalVideo != null && Files.exists(originalVideo))
            return originalVideo;
        return null;
    }

    public static void karaokeVideo(ToolRunner runner,
                                    Path video, Path noVocals, Path assFile,
                                    Path vocals, Path backvocalsFile,
                                    Path outputVideo) throws IOException, InterruptedException {
        runner.println("Adding subtitles...");

        Backvocals bv = JsonUtil.readFile(backvocalsFile, Backvocals.class, () -> Backvocals.EMPTY);
        List<String> backVocals = new RangesToCommands().convert(bv.ranges());

        List<String> videoInput;
        if (video != null) {
            videoInput = List.of(
                "-i", video.toString()
            );
        } else {
            // Используем виртуальное видео длиной 1 час, оно обрезается с помощью опции -shortest до длины аудио:
            videoInput = List.of(
                "-f", "lavfi", "-i", "color=size=1280x720:duration=3600:rate=24:color=black"
            );
        }
        List<String> audioInput = List.of(
            "-i", noVocals.toString()
        );

        List<String> ffmpeg = new ArrayList<>();
        ffmpeg.addAll(videoInput); // input 0
        ffmpeg.addAll(audioInput); // input 1
        String audioSource;
        if (!backVocals.isEmpty() && Files.exists(vocals)) {
            Path commands = outputVideo.resolveSibling("commands.txt");
            Files.write(commands, backVocals);
            ffmpeg.addAll(List.of(
                "-i", vocals.toString(), // input 2
                "-filter_complex", "[2:a]asendcmd=f=" + escapeFilter(commands.toString()) + ",volume@myvol=0:eval=frame[backvocals]; [backvocals][1:a]amix=inputs=2:duration=shortest[mixedout]"
            ));
            audioSource = "[mixedout]"; // audio from inputs 1 & 2
        } else {
            audioSource = "1:a:0"; // audio from input 1
        }
        ffmpeg.addAll(List.of(
            "-y", "-stats",
            "-vf", "ass=" + escapeFilter(assFile.toString()),
            "-map", "0:v:0", // video from input 0
            "-map", audioSource,
            "-shortest"
        ));
        addCodecs(video, ffmpeg);
        ffmpeg.add(outputVideo.toString());
        runner.run().ffmpeg(ffmpeg);
    }
}
