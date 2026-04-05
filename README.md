# KaraEd: Karaoke Editor

KaraEd is an utility for generating karaoke videos from YouTube clips or local media files. It automates the complex process of vocal separation and lyric synchronization using AI models.

## 🚀 Getting Started

**Why KaraEd?**
- **Local & Offline**: KaraEd runs entirely on your machine. You don't need an internet connection to process your files.
- **Precision over Automation**: Automated markup is rarely perfect. KaraEd opts for a "manual but easy" approach, trading a few minutes of effort for significantly better results.
- **Tweakable Outputs**: Your project doesn't end at generation. You can easily tweak subtitles and post-process the resulting video.

Watch a [tutorial video](https://www.youtube.com/watch?v=VCAPqE7yk2I) with a step-by-step guide.

## 🛠 Core Technologies

KaraEd integrates several tools under the hood:
* **[yt-dlp](https://github.com/yt-dlp/yt-dlp)** for video acquisition.
* **[demucs](https://github.com/facebookresearch/demucs)** to separate vocals and instrumentals.
* **[whisperX](https://github.com/m-bain/whisperx)** to assign precise word-level timestamps to lyrics.
* **[FFmpeg](https://www.ffmpeg.org)** for video editing.

---

## 🔄 Project Lifecycle

Creating a karaoke track follows a series of predefined steps:

1. Download source media via `yt-dlp`.
2. Isolate vocals and instrumentals using `demucs`.
3. Manually map vocal segments to the provided lyric lines.
4. Use `whisperX` to assign timestamps to lyrics within those segments.
5. Generate a `subs.ass` draft subtitles file, which can be edited manually (by **[Aegisub](https://github.com/Aegisub/Aegisub)**, for example).
6. Mark "Backvocal" ranges where original vocals should be preserved.
7. Transform the draft subtitles into the final `karaoke.ass` format.
8. Upscale video (optional) and combine the instrumental track, subtitles, and backvocals into the final video.

---

## 💡 Advanced Features

### Handling Vocal Artifacts

If `demucs` incorrectly identifies noise as vocals, you can suppress these segments by marking them with a `#` text line in the `ranges` editor.

### Preserving Backvocals

You can preserve original vocals for specific lyrics by wrapping the text in curly braces `{like this}` during project creation.

### Split parameters

See [wiki on Split Parameters](https://github.com/osobolev/karaed/wiki/Split-parameters) used in the `ranges` editor.

### Local storage

KaraEd is designed to work **locally and offline**, but needs an internet connection on the first run.
* **First Start:** The application automatically downloads FFmpeg, Python, and required audio processing packages. They are stored in `%USERPROFILE%\.karaed`.
* **First Run:** AI models are downloaded upon the first project execution. They are stored in `%USERPROFILE%\.cache`.
* **Subsequent Runs:** No internet connection is required.

### Command line

Use `karaed --help` in your terminal for a full list of available command-line arguments.

### Running on Linux

To build from source (requires **full OpenJDK 21+**, do not use `-headless`):
```
export JAVA_HOME=<path_to_java_21>
./gradlew jre distr
```

The application executable will be located at `distr/karaed/bin/karaed`.

You need **FFmpeg** and a **Python environment** installed to run the application.

On Debian-based distros (Ubuntu, Mint, etc.), run:
```
sudo apt install ffmpeg python3-venv
python3 -m venv ~/karaed_venv
```

Paths to use when setting up the application:
- `/home/<user>/karaed_env/bin` for `Python` and `pip`
- `/usr/bin` for `ffmpeg`

---

## 📂 Project Structure & Output

The final result is rendered as **`karaoke.mp4`** in your project folder.

| File / Path       | Description                                                   |
|:------------------|:--------------------------------------------------------------|
| `htdemucs/audio/` | Contains `vocals.wav` and `no_vocals.wav` (separated tracks). |
| `text.txt`        | The original input lyrics.                                    |
| `subs.ass`        | The subtitle source for manual timing tweaks.                 |
| `karaed.log`      | Technical logs and error reports.                             |
