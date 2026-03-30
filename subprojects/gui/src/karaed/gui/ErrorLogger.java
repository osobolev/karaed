package karaed.gui;

import java.nio.file.Path;

public interface ErrorLogger {

    void error(Throwable ex);

    default ErrorLogger derive(Path projectDir) {
        return this;
    }
}
