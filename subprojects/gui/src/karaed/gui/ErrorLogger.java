package karaed.gui;

import java.nio.file.Path;

public interface ErrorLogger {

    void error(Throwable ex);

    default ErrorLogger derive(Path projectDir) {
        return this;
    }

    default void close() {
    }

    default void setDefault() {
        Thread.currentThread().setUncaughtExceptionHandler((t, ex) -> error(ex));
    }
}
