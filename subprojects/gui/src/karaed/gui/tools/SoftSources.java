package karaed.gui.tools;

interface SoftSources {

    default String pythonUrl() {
        return "https://www.python.org/ftp/python/3.11.9/python-3.11.9-embed-amd64.zip";
    }

    default String getPipUrl() {
        return "https://bootstrap.pypa.io/get-pip.py";
    }

    default String ffmpegUrl() {
        return "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";
    }
}
