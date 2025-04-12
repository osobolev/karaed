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

    protected BasePanel(String title, Supplier<Path> getFile,
                        Class<T> cls, Supplier<T> defValue) throws IOException {
        this.getFile = getFile;

        Path file = getFile.get();
        if (file != null) {
            this.origData = JsonUtil.readFile(file, cls, defValue);
        } else {
            this.origData = defValue.get();
        }

        main.setBorder(BorderFactory.createCompoundBorder(
            title == null ? BorderFactory.createEmptyBorder() : BorderFactory.createTitledBorder(title),
            BorderFactory.createEmptyBorder(5, 5, 0, 0)
        ));
    }

    final JComponent getVisual() {
        return main;
    }

    abstract T newData();

    final void save() throws IOException {
        T newData = newData();
        if (Objects.equals(newData, origData))
            return;
        Path file = getFile.get();
        Files.createDirectories(file.getParent());
        JsonUtil.writeFile(file, newData);
    }
}
