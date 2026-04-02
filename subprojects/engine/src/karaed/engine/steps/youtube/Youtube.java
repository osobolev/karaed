package karaed.engine.steps.youtube;

import karaed.engine.KaraException;
import karaed.engine.formats.ffprobe.FFFormat;
import karaed.engine.formats.ffprobe.FFTags;
import karaed.engine.formats.info.Info;
import karaed.engine.opts.OCut;
import karaed.engine.opts.OInput;
import karaed.engine.video.FileStreamUtil;
import karaed.engine.video.VideoFinder;
import karaed.json.JsonUtil;
import karaed.tools.ToolRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

public final class Youtube {

    private static String getBaseName(Path file, String extension) {
        String fileName = file.getFileName().toString();
        if (!fileName.endsWith(extension))
            throw new KaraException(String.format("Info file name should end with \"%s\"", extension));
        return fileName.substring(0, fileName.length() - extension.length());
    }

    private static Path downloadTo(ToolRunner runner, OInput input, Path infoFile, VideoFinder finder,
                                   String suffix) throws IOException, InterruptedException {
        String basePath = finder.getDir() + File.separator + finder.getBaseName();
        String infoBasePath = infoFile.getParent() + File.separator + getBaseName(infoFile, ".info.json");
        runner.run().pythonTool(
            "yt-dlp",
            "--no-mtime", "--no-playlist",
            "--write-info-json",
            "--output", basePath + "." + suffix + "%(ext)s",
            "--output", "infojson:" + infoBasePath + ".%(ext)s",
            input.url()
        );
        return finder.getVideo(suffix, true);
    }

    private static Info fileMeta(ToolRunner runner, Path file, boolean needExtension) throws IOException, InterruptedException {
        FFFormat format = FileFormatUtil.getFormat(runner, file);
        FFTags tags = format.tags();
        String artist;
        if (tags.artist() != null) {
            artist = tags.artist();
        } else {
            artist = tags.album_artist();
        }
        String ext;
        if (needExtension) {
            String fileName = file.getFileName().toString();
            int p = fileName.lastIndexOf('.');
            ext = p > 0 ? fileName.substring(p + 1) : "";
        } else {
            ext = null;
        }
        return new Info(
            artist, tags.title(), null, null, ext
        );
    }

    private static void cutFile(ToolRunner runner, Path srcFile, Path file, CutRange range, boolean audioOnly) throws IOException, InterruptedException {
        if (range == null) {
            Files.copy(srcFile, file, StandardCopyOption.REPLACE_EXISTING);
            Files.setLastModifiedTime(file, FileTime.from(Instant.now()));
        } else {
            runner.println("Cutting " + (audioOnly ? "audio" : "video") + " file...");
            range.cutFile(runner, srcFile, file, audioOnly);
        }
    }

    private static void extractAudio(ToolRunner runner, Path video, Path audio) throws IOException, InterruptedException {
        runner.println("Extracting audio.mp3...");
        runner.run().ffmpeg(List.of(
            "-y", "-stats",
            "-i", video.toString(),
            "-vn",
            "-b:a", "192k",
            "-f", "mp3",
            audio.toString()
        ));
    }

    public static void download(ToolRunner runner, OInput input, OCut cut, Path audio, Path infoFile, VideoFinder finder) throws IOException, InterruptedException {
        CutRange range = CutRange.create(cut);
        if (input.url() != null) {
            runner.println(String.format("Downloading from Youtube%s...", range == null ? "" : " (range " + range + ")"));
            Path video;
            if (range == null) {
                video = downloadTo(runner, input, infoFile, finder, "");
            } else {
                Path fullVideo = downloadTo(runner, input, infoFile, finder, "full.");

                runner.println("Cutting downloaded video...");
                CutRange realCut = range.toRealCut(runner, fullVideo);
                Path cutVideo = finder.getVideo("", true);
                realCut.cutFile(runner, fullVideo, cutVideo, false);
                Files.delete(fullVideo);

                video = cutVideo;
            }

            extractAudio(runner, video, audio);
        } else if (input.file() != null) {
            Path srcFile = Path.of(input.file());
            boolean audioOnly = FileStreamUtil.listVideoStreams(runner, srcFile).isEmpty();

            Info info = fileMeta(runner, srcFile, !audioOnly);
            JsonUtil.writeFile(infoFile, info);

            if (audioOnly) {
                cutFile(runner, srcFile, audio, range, true);
            } else {
                Path video = finder.getVideo("", true);
                cutFile(runner, srcFile, video, range, false);

                extractAudio(runner, video, audio);
            }
        } else {
            throw new KaraException("Either URL or file must be specified");
        }
    }

    private static Info youtubeMeta(ToolRunner runner, String url) throws IOException, InterruptedException {
        return runner.run(JsonUtil.parser(Info.class)).pythonTool(
            "yt-dlp",
            "--no-playlist",
            "--print-json", "-s", url
        );
    }

    public static Info metaInfo(ToolRunner runner, OInput input) throws IOException, InterruptedException {
        if (input.url() != null) {
            return youtubeMeta(runner, input.url());
        } else if (input.file() != null) {
            return fileMeta(runner, Path.of(input.file()), false);
        } else {
            throw new KaraException("Either URL or file must be specified");
        }
    }
}
