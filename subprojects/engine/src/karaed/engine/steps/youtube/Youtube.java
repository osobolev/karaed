package karaed.engine.steps.youtube;

import karaed.engine.KaraException;
import karaed.engine.formats.ffprobe.FFFormat;
import karaed.engine.formats.ffprobe.FFTags;
import karaed.engine.formats.info.Info;
import karaed.engine.opts.OCut;
import karaed.engine.opts.OInput;
import karaed.engine.video.VideoFinder;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

public final class Youtube {

    private static final String FULL = "full.";

    public static void download(ProcRunner runner, OInput input, OCut cut, Path audio, Path infoFile, VideoFinder finder) throws IOException, InterruptedException {
        CutRange range = CutRange.create(cut);
        if (input.url() != null) {
            String basePath = finder.getDir() + File.separator + finder.getBaseName();
            runner.println(String.format("Downloading from Youtube%s...", range == null ? "" : " (range " + range + ")"));
            Path video;
            if (range == null) {
                runner.runPythonExe(
                    "yt-dlp",
                    "--no-mtime",
                    "--write-info-json",
                    "--output", basePath + ".%(ext)s",
                    "--output", "infojson:" + infoFile,
                    input.url()
                );
                video = finder.getVideo("", true);
            } else {
                runner.runPythonExe(
                    "yt-dlp",
                    "--no-mtime",
                    "--write-info-json",
                    "--output", basePath + "." + FULL + "%(ext)s",
                    "--output", "infojson:" + infoFile,
                    input.url()
                );
                Path fullVideo = finder.getVideo(FULL, true);

                runner.println("Cutting downloaded video...");
                CutRange realCut = new KeyRangeDetector(runner, range).getRealCut(fullVideo);
                Path cutVideo = finder.getVideo("", true);
                realCut.cutFile(runner, fullVideo, cutVideo);
                Files.delete(fullVideo);

                video = cutVideo;
            }
            runner.println("Extracting audio.mp3...");
            runner.runFFMPEG(List.of(
                "-y", "-stats",
                "-i", video.toString(),
                "-vn",
                "-b:a", "192k",
                "-f", "mp3",
                audio.toString()
            ));
        } else if (input.file() != null) {
            Path srcFile = Path.of(input.file());
            if (range == null) {
                Files.copy(srcFile, audio, StandardCopyOption.REPLACE_EXISTING);
                Files.setLastModifiedTime(audio, FileTime.from(Instant.now()));
            } else {
                range.cutFile(runner, srcFile, audio);
            }
            FFFormat format = FileFormatUtil.getFormat(runner, audio);
            FFTags tags = format.tags();
            String artist;
            if (tags.artist() != null) {
                artist = tags.artist();
            } else {
                artist = tags.album_artist();
            }
            Info info = new Info(
                artist, tags.title(), null, null, null
            );
            JsonUtil.writeFile(infoFile, info);
        } else {
            throw new KaraException("Either URL or file must be specified");
        }
    }
}
