package karaed.gui;

import karaed.gui.tools.SetupTools;

import java.nio.file.Path;

public record AppContext(
    ErrorLogger mainLogger,
    SetupTools tools,
    Path rootDir
) {}
