package karaed.gui.tools;

import karaed.gui.util.InputUtil;

import javax.swing.*;

final class ToolRow {

    final JLabel lblName;
    final JTextField tfVersion = new JTextField(15);
    final JButton btnUpdate = new JButton("Update");
    private String newVersion = null;

    ToolRow(Tool tool) {
        this.lblName = new JLabel(tool + ":");
        tfVersion.setEditable(false);
    }

    boolean isInstalled() {
        String currentVersion = tfVersion.getText();
        return !currentVersion.isEmpty();
    }

    boolean enableUpdate() {
        String currentVersion = tfVersion.getText();
        boolean installed = !currentVersion.isEmpty();
        boolean canUpdate;
        if (!installed || newVersion == null) {
            canUpdate = false;
        } else {
            canUpdate = !currentVersion.equals(newVersion);
        }
        btnUpdate.setEnabled(canUpdate);
        if (canUpdate) {
            btnUpdate.setToolTipText("Update to " + newVersion);
        } else {
            btnUpdate.setToolTipText(null);
        }
        return installed;
    }

    void setCurrentVersion(String version) {
        if (version != null) {
            InputUtil.setText(tfVersion, version);
        }
    }

    void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }
}
