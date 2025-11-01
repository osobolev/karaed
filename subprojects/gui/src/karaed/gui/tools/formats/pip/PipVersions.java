package karaed.gui.tools.formats.pip;

import com.google.gson.annotations.SerializedName;

public record PipVersions(
    String name,
    String[] versions,
    String latest,
    @SerializedName("installed_version")
    String installed
) {}
