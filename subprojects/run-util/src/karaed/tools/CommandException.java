package karaed.tools;

import java.io.IOException;
import java.util.List;

public class CommandException extends IOException {

    public final List<String> commandLine;

    public CommandException(String message, List<String> commandLine) {
        super(message);
        this.commandLine = commandLine;
    }
}
