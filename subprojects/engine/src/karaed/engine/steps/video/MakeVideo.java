package karaed.engine.steps.video;

import karaed.engine.formats.ffprobe.FFStream;
import karaed.engine.formats.ffprobe.FFStreams;
import karaed.engine.video.VideoFinder;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MakeVideo {

    public static final String PREPARED = "prepared.";

    private static void addCodecs(List<String> args) {
        args.addAll(List.of(
            "-c:v", "libx264",
            "-c:a", "aac",
            "-b:a", "192k"
        ));
    }

    private static void enlargeVideo(ProcRunner runner, Path video, Path largeVideo, int width, int height) throws IOException, InterruptedException {
        String newSize = width + "x" + height;
        runner.println(String.format("Enlarging small video to %s...", newSize));
        List<String> ffmpeg = new ArrayList<>(List.of(
            "-i", video.toString(),
            "-y", "-stats",
            "-s", newSize
        ));
        addCodecs(ffmpeg);
        ffmpeg.add(largeVideo.toString());
        runner.runFFMPEG(ffmpeg);
    }

    public static void prepareVideo(ProcRunner runner, VideoFinder finder) throws IOException, InterruptedException {
        Path video = finder.getVideo("", false);
        if (video == null || !Files.exists(video))
            return;
        FFStreams streams = runner.runFFProbe(
            List.of(
                "-print_format", "json",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                video.toString()
            ),
            stdout -> JsonUtil.parse(stdout, FFStreams.class)
        );
        Path preparedVideo = finder.getVideo(PREPARED, true);
        FFStream stream = streams.streams().getFirst();
        int width = stream.width();
        int height = stream.height();
        if (width < 1280) {
            int scale = (int) Math.ceil(1280.0 / width);
            int newWidth = scale * width;
            int newHeight = scale * height;
            enlargeVideo(runner, video, preparedVideo, newWidth, newHeight);
        } else {
            Files.write(preparedVideo, new byte[0]);
        }
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

    public static void karaokeVideo(ProcRunner runner,
                                    Path video, Path noVocals, Path assFile,
                                    Path outputVideo) throws IOException, InterruptedException {
        runner.println("Adding subtitles...");

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
        // todo: add some vocals parts if necessary
        ffmpeg.addAll(List.of(
            "-y", "-stats",
            "-vf", "ass=" + escapeFilter(assFile.toString()),
            "-map", "0:v:0", // video from input 0
            "-map", "1:a:0", // audio from input 1
            "-shortest"
        ));
        addCodecs(ffmpeg);
        ffmpeg.add(outputVideo.toString());
        runner.runFFMPEG(ffmpeg);
    }
}
