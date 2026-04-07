package karaed.gui.options;

import karaed.engine.opts.OInput;
import karaed.gui.util.SimpleGlassPane;
import karaed.tools.ToolRunner;

import javax.swing.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class InputDetailsFetcher<R> {

    private final OptCtx ctx;
    private final InputPanel inputPanel;

    InputDetailsFetcher(OptCtx ctx, InputPanel inputPanel) {
        this.ctx = ctx;
        this.inputPanel = inputPanel;
    }

    interface FetchSupplier<R> {
        
        R fetch(ToolRunner runner, OInput input) throws Exception;
    }

    void fetch(boolean silenceErrors,
               FetchSupplier<R> fetcher,
               Consumer<R> showResult,
               Predicate<Throwable> customHandler) {
        OInput input;
        try {
            input = inputPanel.newData();
        } catch (ValidationException ex) {
            if (!silenceErrors) {
                ex.show(ctx.owner);
            }
            return;
        }
        SimpleGlassPane glass = (SimpleGlassPane) ctx.owner.toRootPane().getGlassPane();
        glass.show("Loading...");
        new SwingWorker<R, Void>() {

            @Override
            protected R doInBackground() throws Exception {
                return fetcher.fetch(ctx.runner(), input);
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
                        if (!customHandler.test(error)) {
                            ctx.owner.error(error);
                        }
                    }
                }
            }
        }.execute();
    }
}
