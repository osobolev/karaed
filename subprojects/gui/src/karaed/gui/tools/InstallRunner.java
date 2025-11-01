package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class InstallRunner {

    private final SetupTools tools;
    private final ToolRunner runner;

    InstallRunner(SetupTools tools, ToolRunner runner) {
        this.tools = tools;
        this.runner = runner;
    }

    private void log(String message) {
        runner.println(message);
    }

    private interface ContentHandler {

        void accept(InputStream is) throws IOException;
    }

    private static void download(String url, ContentHandler handler) throws IOException {
        URLConnection conn = URI.create(url).toURL().openConnection();
        try (InputStream is = conn.getInputStream()) {
            handler.accept(is);
        }
        if (conn instanceof HttpURLConnection http) {
            http.disconnect();
        }
    }

    private static void downloadZip(String url,
                                    Function<String, Path> getDest,
                                    Function<Path, List<String>> replace) throws IOException {
        download(url, is -> {
            try (ZipInputStream zis = new ZipInputStream(is)) {
                while (true) {
                    ZipEntry e = zis.getNextEntry();
                    if (e == null)
                        break;
                    if (e.isDirectory())
                        continue;
                    Path dest = getDest.apply(e.getName());
                    if (dest == null)
                        continue;
                    Files.createDirectories(dest.getParent());
                    List<String> lines = replace.apply(dest);
                    if (lines != null) {
                        Files.write(dest, lines);
                    } else {
                        Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        });
    }

    private void installPython() throws IOException {
        log("Downloading Python...");
        downloadZip(
            tools.sources.pythonUrl(), tools.pythonDir()::resolve,
            file -> {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith("._pth")) {
                    return List.of(
                        "python310.zip", // todo!!!
                        ".",
                        "import site"
                    );
                }
                return null;
            }
        );
    }

    private void installPIP() throws IOException, InterruptedException {
        log("Installing PIP...");
        Path getPip = tools.pythonDir().resolve("get-pip.py");
        download(tools.sources.getPipUrl(), is -> Files.copy(is, getPip, StandardCopyOption.REPLACE_EXISTING));

        runner.run().python("get-pip", getPip.toString(), "--no-warn-script-location");
    }

    private void installPackages(Collection<Tool> toInstall) throws IOException, InterruptedException {
        log("Installing required packages...");
        List<String> args = new ArrayList<>(List.of("-v", "--no-warn-script-location", "install"));
        toInstall.stream().map(Tool::packName).forEach(args::add);
        runner.run().pythonTool("pip", args);
    }

    void installFFMPEG() throws IOException {
        log("Downloading FFMPEG...");
        downloadZip(
            tools.sources.ffmpegUrl(),
            name -> {
                Path sub = Path.of(name);
                int len = sub.getNameCount();
                if (len <= 1)
                    return null;
                return tools.ffmpegDir().resolve(sub.subpath(1, len));
            },
            file -> null
        );
    }

    private void require(Tool... tools) {
        for (Tool tool : tools) {
            if (!this.tools.installed(tool))
                throw new IllegalStateException(tool + " is not installed!");
        }
    }

    void install(Set<Tool> tools) throws IOException, InterruptedException {
        Set<Tool> packs = EnumSet.noneOf(Tool.class);
        for (Tool tool : tools) {
            String packName = tool.maybePackName();
            if (packName == null) {
                packs.add(tool);
            }
        }
        if (tools.contains(Tool.PYTHON)) {
            installPython();
        }
        if (tools.contains(Tool.FFMPEG)) {
            installFFMPEG();
        }
        if (tools.contains(Tool.PIP)) {
            require(Tool.PYTHON);
            installPIP();
        }
        if (!packs.isEmpty()) {
            require(Tool.PYTHON, Tool.PIP);
            installPackages(packs);
        }
    }
}
