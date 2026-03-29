package karaed.gui.tools;

import karaed.tools.ToolRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class WindowsInstallRunner extends InstallRunner {

    private final WindowsSetupTools tools;
    private final WindowsSoftSources sources;

    WindowsInstallRunner(WindowsSetupContext ctx, ToolRunner runner) {
        super(ctx.tools, runner);
        this.tools = ctx.wintools;
        this.sources = ctx.sources;
    }

    private final class ProgressLogger implements AutoCloseable {

        private final String title;

        ProgressLogger(String title) {
            this.title = title;
            runner.log(false, title + "...");
        }

        void setProgress(int percent) {
            runner.log(false, "\r" + title + "... " + percent + "%");
        }

        @Override
        public void close() {
            log("\r" + title + "... DONE");
        }
    }

    private interface SpecialHandling {

        boolean handle(Path dest, InputStream content) throws IOException;
    }

    private void downloadZip(String name, String url,
                             Function<String, Path> getDest,
                             SpecialHandling specialHandling) throws IOException, InterruptedException {
        try (ProgressLogger progress = new ProgressLogger("Downloading " + name)) {
            Download.download(
                url, progress::setProgress,
                is -> {
                    try (ZipInputStream zis = new ZipInputStream(is)) {
                        while (true) {
                            if (Thread.currentThread().isInterrupted())
                                throw new InterruptedException();
                            ZipEntry e = zis.getNextEntry();
                            if (e == null)
                                break;
                            if (e.isDirectory())
                                continue;
                            Path dest = getDest.apply(e.getName());
                            if (dest == null)
                                continue;
                            Files.createDirectories(dest.getParent());
                            if (specialHandling.handle(dest, zis))
                                continue;
                            Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            );
        }
    }

    private static List<String> uncommentSite(InputStream is) throws IOException {
        // Do not close BufferedReader, since we need to keep InputStream open:
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String importSite = "import site";
        List<String> newLines = new ArrayList<>();
        while (true) {
            String line = br.readLine();
            if (line == null)
                break;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            if (trimmed.equals(importSite)) {
                // Already contents "import site"
                return null;
            }
            newLines.add(line);
        }
        newLines.add(importSite);
        return newLines;
    }

    private void installPython() throws IOException, InterruptedException {
        downloadZip(
            "Python", sources.pythonUrl(), tools.pythonDir()::resolve,
            (file, content) -> {
                String fileName = file.getFileName().toString();
                if (fileName.endsWith("._pth")) {
                    List<String> newLines = uncommentSite(content);
                    if (newLines != null) {
                        Files.write(file, newLines);
                        return true;
                    }
                }
                return false;
            }
        );
    }

    private void installPIP() throws IOException, InterruptedException {
        Path getPip = tools.pythonDir().resolve("get-pip.py");
        try (ProgressLogger progress = new ProgressLogger("Installing PIP")) {
            Download.download(
                sources.getPipUrl(), progress::setProgress,
                is -> Files.copy(is, getPip, StandardCopyOption.REPLACE_EXISTING)
            );
        }

        runner.run().python("get-pip", getPip.toString(), "--no-warn-script-location");
    }

    void installFFMPEG() throws IOException, InterruptedException {
        downloadZip(
            "FFMPEG", sources.ffmpegUrl(),
            name -> {
                Path sub = Path.of(name);
                int len = sub.getNameCount();
                if (len <= 1)
                    return null;
                return tools.ffmpegDir().resolve(sub.subpath(1, len));
            },
            (file, content) -> false
        );
    }

    @Override
    void install(Set<Tool> tools) throws IOException, InterruptedException {
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
        installMissingPackages(tools);
    }
}
