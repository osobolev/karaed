package karaed.tools;

import java.io.IOException;
import java.io.Writer;

public interface OutputFactory {

    Writer open() throws IOException;
}
