package karaed.gui.options;

import karaed.json.JsonUtil;

import javax.swing.*;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;

abstract class BasePanel<T> {

    private final Supplier<Path> getFile;
    final T origData;
    final JPanel main = new JPanel(new GridBagLayout());

    interface DataReader<T> {

        T read(Path file) throws IOException;
    }

    protected BasePanel(String title, Supplier<Path> getFile,
                        DataReader<T> reader, Supplier<T> defValue) throws IOException {
        this.getFile = getFile;

        Path file = getFile.get();
        if (file != null && Files.exists(file)) {
            this.origData = reader.read(file);
        } else {
            this.origData = defValue.get();
        }

        main.setBorder(BorderFactory.createCompoundBorder(
            title == null ? BorderFactory.createEmptyBorder() : BorderFactory.createTitledBorder(title),
            BorderFactory.createEmptyBorder(5, 5, 0, 0)
        ));
    }

    protected BasePanel(String title, Supplier<Path> getFile,
                        Class<T> cls, Supplier<T> defValue) throws IOException {
        this(title, getFile, file -> JsonUtil.readFile(file, cls), defValue);
    }

    final JComponent getVisual() {
        return main;
    }

    abstract T newData() throws ValidationException;

    void writeData(Path file, T data) throws IOException {
        JsonUtil.writeFile(file, data);
    }

    abstract static class Saver {

        abstract void save() throws IOException;
    }

    final Saver prepareToSave() throws ValidationException {
        T newData = newData();
        return new Saver() {
            @Override
            void save() throws IOException {
                if (Objects.equals(newData, origData))
                    return;
                Path file = getFile.get();
                Files.createDirectories(file.getParent());
                writeData(file, newData);
            }
        };
    }
}
