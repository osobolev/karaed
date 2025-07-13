package karaed.tools;

import java.io.IOException;
import java.io.Reader;

public interface OutputProcessor<T> {

    T process(Reader rdr) throws IOException;
}
