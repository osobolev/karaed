package karaed.gui.tools.formats.ffprobe;

import com.google.gson.annotations.SerializedName;

public record FFVersion(
    @SerializedName("program_version")
    ProgramVersion programVersion
) {}
