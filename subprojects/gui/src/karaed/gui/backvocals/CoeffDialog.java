package karaed.gui.backvocals;

import karaed.gui.util.BaseDialog;
import karaed.gui.util.BaseWindow;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

final class CoeffDialog extends BaseDialog {

    private final JSpinner chVolume = new JSpinner(new SpinnerNumberModel(100, 0, 200, 1));

    private Integer result = null;

    CoeffDialog(BaseWindow owner, Double coeff) {
        super(owner, "Backvocals volume");

        if (coeff != null) {
            chVolume.setValue(BackvocalPainter.toPercent(coeff));
        }

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
        center.add(new JLabel("Backvocals volume:"));
        center.add(chVolume);
        center.add(new JLabel("%"));
        add(center, BorderLayout.CENTER);

        JPanel butt = new JPanel();
        butt.add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = (Integer) chVolume.getValue();
                dispose();
            }
        }));
        butt.add(new JButton(new AbstractAction("Cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        }));
        add(butt, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    boolean isOk() {
        return result != null;
    }

    Double getCoeff() {
        if (result == null)
            return null;
        int percent = result.intValue();
        return percent == 100 ? null : percent / 100.0;
    }
}
