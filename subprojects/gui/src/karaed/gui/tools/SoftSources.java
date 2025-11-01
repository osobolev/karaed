package karaed.gui.tools;

record SoftSources(
    String pythonUrl,
    String getPipUrl,
    String ffmpegUrl
) {

    SoftSources() {
        this(
            "https://www.python.org/ftp/python/3.10.11/python-3.10.11-embed-amd64.zip", // todo: update it???
            "https://bootstrap.pypa.io/get-pip.py",
            "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip"
        );
    }
}
