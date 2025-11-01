package karaed.gui.tools;

interface SoftSources {

    default String pythonUrl() {
        // >=3.10, <3.14
        // 3.10.19 09-Oct-2025
        // 3.11.14
        // 3.12.12
        // 3.13.9
        return "https://www.python.org/ftp/python/3.10.11/python-3.10.11-embed-amd64.zip"; // todo: update it???!!!
    }

    default String getPipUrl() {
        return "https://bootstrap.pypa.io/get-pip.py";
    }

    default String ffmpegUrl() {
        return "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";
    }
}
