package karaed.gui.align;

import karaed.engine.formats.ranges.AreaParams;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class ParamsComponent {

    private static final float MILLIS_SCALE = 1000f;

    private final JLabel lblThreshold = new JLabel("Silence threshold, %:");
    private final JSpinner chThreshold = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
    private final JLabel lblSilenceGap = new JLabel("Max silence gap, millis:");
    private final JSpinner chSilenceGap = new JSpinner(new SpinnerNumberModel(10, 10, 1000, 10));
    private final JCheckBox cbRangeDuration = new JCheckBox("Min range duration, millis:");
    private final JSpinner chRangeDuration = new JSpinner(new SpinnerNumberModel(10, 10, 1000, 10));
    private final JPanel main = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

    private final List<Consumer<AreaParams>> listeners = new ArrayList<>();

    private boolean enabled = true;
    private boolean ignoreChanges = false;

    ParamsComponent() {
        ChangeListener changeListener = e -> {
            if (ignoreChanges)
                return;
            if (e.getSource() == chSilenceGap && !cbRangeDuration.isSelected()) {
                runWithoutEvents(() -> {
                    Object maxSilenceGap = chSilenceGap.getValue();
                    chRangeDuration.setValue(maxSilenceGap);
                });
            }
            fireParamsChanged();
        };
        chThreshold.addChangeListener(changeListener);
        chSilenceGap.addChangeListener(changeListener);
        chRangeDuration.addChangeListener(changeListener);
        cbRangeDuration.addActionListener(e -> {
            enableDisableRangeDuration();
            if (!cbRangeDuration.isSelected()) {
                runWithoutEvents(() -> {
                    Object maxSilenceGap = chSilenceGap.getValue();
                    Object minRangeDuration = chRangeDuration.getValue();
                    if (!Objects.equals(maxSilenceGap, minRangeDuration)) {
                        chRangeDuration.setValue(maxSilenceGap);
                        fireParamsChanged();
                    }
                });
            }
        });

        main.add(lblThreshold);
        main.add(chThreshold);
        main.add(lblSilenceGap);
        main.add(chSilenceGap);
        main.add(cbRangeDuration);
        main.add(chRangeDuration);
    }

    private void fireParamsChanged() {
        AreaParams params = getParams();
        for (Consumer<AreaParams> listener : listeners) {
            listener.accept(params);
        }
    }

    JComponent getVisual() {
        return main;
    }

    void addListener(Consumer<AreaParams> listener) {
        listeners.add(listener);
    }

    private void runWithoutEvents(Runnable code) {
        ignoreChanges = true;
        try {
            code.run();
        } finally {
            ignoreChanges = false;
        }
    }

    void setParams(AreaParams params) {
        runWithoutEvents(() -> {
            chThreshold.setValue(params.silenceThreshold());
            int maxSilenceGap = Math.round(params.maxSilenceGap() * MILLIS_SCALE);
            int minRangeDuration = Math.round(params.minRangeDuration() * MILLIS_SCALE);
            chSilenceGap.setValue(maxSilenceGap);
            cbRangeDuration.setSelected(maxSilenceGap != minRangeDuration);
            chRangeDuration.setValue(minRangeDuration);
            enableDisableRangeDuration();
        });
    }

    private void enableDisableRangeDuration() {
        chRangeDuration.setEnabled(cbRangeDuration.isSelected() && enabled);
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
        this.enabled = on;
        lblThreshold.setEnabled(on);
        chThreshold.setEnabled(on);
        lblSilenceGap.setEnabled(on);
        chSilenceGap.setEnabled(on);
        cbRangeDuration.setEnabled(on);
        enableDisableRangeDuration();
    }
}
