package karaed.gui;

import karaed.gui.tools.SetupTools;

import java.nio.file.Path;

public record AppContext(
    ErrorLogger logger,
    SetupTools tools,
    Path rootDir
) {}
