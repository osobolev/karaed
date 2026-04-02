package karaed.gui.options;

import karaed.engine.opts.OInput;
import karaed.gui.tools.SetupTools;
import karaed.gui.util.BaseWindow;
import karaed.gui.util.SimpleGlassPane;
import karaed.tools.ToolRunner;

import javax.swing.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class InputDetailsFetcher<R> {

    private final BaseWindow owner;
    private final SetupTools tools;
    private final InputPanel inputPanel;

    InputDetailsFetcher(BaseWindow owner, SetupTools tools, InputPanel inputPanel) {
        this.owner = owner;
        this.tools = tools;
        this.inputPanel = inputPanel;
    }

    interface FetchSupplier<R> {
        
        R fetch(ToolRunner runner, OInput input) throws Exception;
    }

    void fetch(boolean silenceErrors,
               FetchSupplier<R> fetcher,
               Consumer<R> showResult,
               Predicate<Throwable> isMessageError) {
        OInput input;
        try {
            input = inputPanel.newData();
        } catch (ValidationException ex) {
            if (!silenceErrors) {
                ex.show(owner);
            }
            return;
        }
        SimpleGlassPane glass = (SimpleGlassPane) owner.toRootPane().getGlassPane();
        glass.show("Loading...");
        new SwingWorker<R, Void>() {

            @Override
            protected R doInBackground() throws Exception {
                ToolRunner runner = new ToolRunner(tools, (stderr, text) -> {});
                return fetcher.fetch(runner, input);
            }

            @Override
            protected void done() {
                glass.setVisible(false);
                try {
                    R result = get();
                    showResult.accept(result);
                } catch (InterruptedException ex) {
                    // ignore
                } catch (ExecutionException ex) {
                    if (!silenceErrors) {
                        Throwable error = ex.getCause();
                        if (isMessageError.test(error)) {
                            owner.error(error.getMessage());
                        } else {
                            owner.error(error);
                        }
                    }
                }
            }
        }.execute();
    }
}
