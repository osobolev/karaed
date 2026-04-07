package karaed.engine.lyrics;

import karaed.engine.formats.info.Info;

public final class LRCException extends Exception {

    public final Info info;

    public LRCException(String message, Info info) {
        super(message);
        this.info = info;
    }
}
