package karaed.gui.options;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;
import java.util.function.Function;

final class FileDnD {

    static void allowDrop(JTextComponent tf, Function<File, File> check) {
        TransferHandler th = tf.getTransferHandler();
        tf.setTransferHandler(new TransferHandler() {

            private boolean isAvailable() {
                return tf.isEditable() && tf.isEnabled();
            }

            @Override
            public boolean canImport(TransferSupport support) {
                if (isAvailable() && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                    return true;
                return th.canImport(support);
            }

            private boolean tryImport(TransferSupport support) {
                Transferable transferable = support.getTransferable();
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                    return false;
                try {
                    List<?> files = (List<?>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() > 0 && files.getFirst() instanceof File file) {
                        File path = check.apply(file);
                        if (path != null) {
                            tf.setText(path.getAbsolutePath());
                            return true;
                        }
                    }
                } catch (Exception ex) {
                    // ignore
                }
                return false;
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (isAvailable() && tryImport(support))
                    return true;
                return th.importData(support);
            }
        });
    }
}
