package karaed.engine.steps.youtube;

import karaed.engine.KaraException;
import karaed.engine.formats.ffprobe.FFFormat;
import karaed.engine.formats.ffprobe.FFTags;
import karaed.engine.formats.info.Info;
import karaed.engine.opts.OCut;
import karaed.engine.opts.OInput;
import karaed.json.JsonUtil;
import karaed.tools.ProcRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Youtube {

    private static String nameWithoutExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return name;
        } else {
            return name.substring(0, dot);
        }
    }

    private static Path getInfoFile(Path audio, String base) {
        return audio.resolveSibling(base + ".info.json");
    }

    private static String getVideoExt(Path audio, String base) throws IOException {
        Path infoFile = getInfoFile(audio, base);
        Info info = JsonUtil.readFile(infoFile, Info.class);
        return info.ext();
    }

    private static Path getVideoFile(Path audio, String base, String ext) {
        return audio.resolveSibling(base + "." + ext);
    }

    public static Path download(ProcRunner runner, OInput input, OCut cut, Path audio) throws IOException, InterruptedException {
        CutRange range = CutRange.create(cut);
        String base = nameWithoutExtension(audio);
        if (input.url() != null) {
            String basePath = audio.getParent().resolve(base).toString();
            runner.println(String.format("Downloading from Youtube%s...", range == null ? "" : " (range " + range + ")"));
            if (range == null) {
                runner.runPythonExe(
                    "yt-dlp",
                    "--write-info-json", "-k",
                    "--extract-audio",
                    "--audio-format", "mp3",
                    "--output", basePath + ".%(ext)s",
                    input.url()
                );
                String ext = getVideoExt(audio, base);
                return getVideoFile(audio, base, ext);
            } else {
                runner.runPythonExe(
                    "yt-dlp",
                    "--write-info-json",
                    "--output", basePath + ".full.%(ext)s",
                    "--output", "infojson:" + basePath + ".%(ext)s",
                    input.url()
                );
                String ext = getVideoExt(audio, base);
                Path fullVideo = getVideoFile(audio, base, "full." + ext);

                runner.println("Cutting downloaded video...");
                CutRange realCut = new KeyRangeDetector(runner, range).getRealCut(fullVideo);
                Path cutVideo = getVideoFile(audio, base, ext);
                realCut.cutFile(runner, fullVideo, cutVideo);
                Files.delete(fullVideo);

                runner.runFFMPEG(List.of(
                    "-y", "-stats",
                    "-i", cutVideo.toString(),
                    "-vn",
                    "-b:a", "192k",
                    "-f", "mp3",
                    audio.toString()
                ));
                return cutVideo;
            }
        } else if (input.file() != null) {
            Path srcFile = Path.of(input.file());
            if (range == null) {
                Files.copy(srcFile, audio);
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
            JsonUtil.writeFile(getInfoFile(audio, base), info);
            return null;
        } else {
            throw new KaraException("Either URL or file must be specified");
        }
    }
}
