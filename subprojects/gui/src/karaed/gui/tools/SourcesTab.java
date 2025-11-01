package karaed.gui.tools;

import karaed.gui.util.InputUtil;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

final class SourcesTab implements SoftSources {

    private final JTextField tfPytonURL = new JTextField(40);
    private final JTextField tfPipURL = new JTextField(40);
    private final JTextField tfFFURL = new JTextField(40);
    private final JPanel main = new JPanel(new GridBagLayout());

    SourcesTab() {
        InputUtil.setText(tfPytonURL, SoftSources.super.pythonUrl());
        InputUtil.setText(tfPipURL, SoftSources.super.getPipUrl());
        InputUtil.setText(tfFFURL, SoftSources.super.ffmpegUrl());

        addRow(0, null, new JLabel("Source URL:"));
        addRow(1, "Python", tfPytonURL);
        addRow(2, "get-pip.py", tfPipURL);
        addRow(3, "ffmpeg", tfFFURL);
        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));
    }

    private void addRow(int i, String label, JComponent field) {
        if (label != null) {
            main.add(new JLabel(label + ":"), new GridBagConstraints(
                0, i, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 5, 5), 0, 0
            ));
        }
        main.add(field, new GridBagConstraints(
            1, i, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 5, 5), 0, 0
        ));
    }

    JComponent getVisual() {
        return main;
    }

    @Override
    public String pythonUrl() {
        return tfPytonURL.getText();
    }

    @Override
    public String getPipUrl() {
        return tfPipURL.getText();
    }

    @Override
    public String ffmpegUrl() {
        return tfFFURL.getText();
    }
}
