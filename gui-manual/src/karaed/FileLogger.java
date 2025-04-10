package karaed;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FileLogger implements ErrorLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private final String fileName;

    private PrintWriter out = null;

    public FileLogger(String fileName) {
        this.fileName = fileName;
    }

    private static String getTimestamp() {
        return TIMESTAMP_FORMAT.format(LocalDateTime.now());
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private PrintWriter getOut() {
        if (out == null) {
            PrintWriter pw;
            try {
                Path path = Path.of(fileName);
                BufferedWriter w = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                pw = new PrintWriter(w);
                String startMessage = "Log started: " + getTimestamp();
                pw.println("------------------ " + startMessage + " ------------------");
            } catch (IOException ex1) {
                pw = new PrintWriter(System.err);
            }
            out = pw;
        }
        return out;
    }

    private static void printMessage(PrintWriter pw, String type, String message) {
        pw.println("[" + type + "] " + getTimestamp() + (message == null ? "" : " | " + message));
    }

    @Override
    public void error(Throwable ex) {
        PrintWriter out = getOut();
        printMessage(out, "ERROR", ex.toString());
        ex.printStackTrace(out);
        out.println("-------------------------------");
    }
}
