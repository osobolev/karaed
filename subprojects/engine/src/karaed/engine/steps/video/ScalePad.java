package karaed.engine.steps.video;

public record ScalePad(
    int scaleWidth,
    int scaleHeight,
    int resultWidth,
    int resultHeight
) {

    private static final int PREF_WIDTH = 1280;
    private static final int PREF_HEIGHT = 720;

    // todo: never scale down???
    public static ScalePad create(int origWidth, int origHeight) {
        double wscale = (double) PREF_WIDTH / origWidth;
        double hscale = (double) PREF_HEIGHT / origHeight;
        if (wscale <= hscale) {
            // scale to width=1280, pad vertically to 720
            int sheight = (int) Math.round(wscale * origHeight);
            return new ScalePad(
                PREF_WIDTH, sheight,
                PREF_WIDTH, PREF_HEIGHT
            );
        } else {
            // scale to height=720, pad horizontally to 1280
            int swidth = (int) Math.round(hscale * origWidth);
            return new ScalePad(
                swidth, PREF_HEIGHT,
                PREF_WIDTH, PREF_HEIGHT
            );
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(resultWidth + "x" + resultHeight);
        int px = resultWidth - scaleWidth;
        int py = resultHeight - scaleHeight;
        if (px != 0 || py != 0) {
            buf.append(" with");
            if (px != 0) {
                buf.append(" x-padding " + px);
            }
            if (py != 0) {
                buf.append(" y-padding " + py);
            }
        }
        return buf.toString();
    }

    String getFormat() {
        StringBuilder buf = new StringBuilder(String.format(
            "scale=w=%s:h=%s",
            scaleWidth, scaleHeight
        ));
        int px = (resultWidth - scaleWidth) / 2;
        int py = (resultHeight - scaleHeight) / 2;
        if (px > 0 || py > 0) {
            buf.append(String.format(
                ",pad=w=%s:h=%s:x=%s:y=%s",
                resultWidth, resultHeight,
                px, py
            ));
        }
        return buf.toString();
    }
}
