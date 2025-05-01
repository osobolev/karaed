package karaed.gui.align;

import karaed.engine.formats.ranges.AreaParams;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ParamsComponent {

    private static final float MILLIS_SCALE = 1000f;

    private final JLabel lblThreshold = new JLabel("Silence threshold, %:");
    private final JSpinner chThreshold = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final JLabel lblSilenceGap = new JLabel("Max silence gap, millis:");
    private final JSpinner chSilenceGap = new JSpinner(new SpinnerNumberModel(10, 10, 1000, 10));
    private final JLabel lblRangeDuration = new JLabel("Min range duration, millis:");
    // todo: correlate chRangeDuration with chSilenceGap???
    private final JSpinner chRangeDuration = new JSpinner(new SpinnerNumberModel(10, 10, 1000, 10));
    private final JPanel main = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

    private final List<Consumer<AreaParams>> listeners = new ArrayList<>();

    private boolean ignoreChanges = false;

    ParamsComponent() {
        ChangeListener changeListener = e -> {
            if (ignoreChanges)
                return;
            AreaParams params = getParams();
            for (Consumer<AreaParams> listener : listeners) {
                listener.accept(params);
            }
        };
        chThreshold.addChangeListener(changeListener);
        chSilenceGap.addChangeListener(changeListener);
        chRangeDuration.addChangeListener(changeListener);

        main.add(lblThreshold);
        main.add(chThreshold);
        main.add(lblSilenceGap);
        main.add(chSilenceGap);
        main.add(lblRangeDuration);
        main.add(chRangeDuration);
    }

    JComponent getVisual() {
        return main;
    }

    void addListener(Consumer<AreaParams> listener) {
        listeners.add(listener);
    }

    void setParams(AreaParams params) {
        ignoreChanges = true;
        try {
            chThreshold.setValue(params.silenceThreshold());
            chSilenceGap.setValue(Math.round(params.maxSilenceGap() * MILLIS_SCALE));
            chRangeDuration.setValue(Math.round(params.minRangeDuration() * MILLIS_SCALE));
        } finally {
            ignoreChanges = false;
        }
    }

    private static Number getNumberValue(JSpinner spinner) {
        return (Number) spinner.getValue();
    }

    private static float getValue(JSpinner spinner, float scale) {
        Number threshold = getNumberValue(spinner);
        return threshold.floatValue() / scale;
    }

    AreaParams getParams() {
        int silenceThreshold = getNumberValue(chThreshold).intValue();
        float maxSilenceGap = getValue(chSilenceGap, MILLIS_SCALE);
        float minRangeDuration = getValue(chRangeDuration, MILLIS_SCALE);
        return new AreaParams(silenceThreshold, maxSilenceGap, minRangeDuration);
    }

    void setEnabled(boolean on) {
        lblThreshold.setEnabled(on);
        chThreshold.setEnabled(on);
        lblSilenceGap.setEnabled(on);
        chSilenceGap.setEnabled(on);
        lblRangeDuration.setEnabled(on);
        chRangeDuration.setEnabled(on);
    }
}
