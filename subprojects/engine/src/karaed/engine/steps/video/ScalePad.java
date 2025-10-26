package karaed.engine.steps.video;

public record ScalePad(
    int scaleWidth,
    int scaleHeight,
    int resultWidth,
    int resultHeight
) {

    private static final int PREF_WIDTH = 1280;
    private static final int PREF_HEIGHT = 720;
    private static final double PREF_RATIO = (double) PREF_WIDTH / PREF_HEIGHT;

    public static ScalePad create(int origWidth, int origHeight) {
        double wscale = (double) PREF_WIDTH / origWidth;
        double hscale = (double) PREF_HEIGHT / origHeight;
        double scale = Math.max(1.0, Math.min(wscale, hscale));
        int scaleWidth = (int) Math.ceil(origWidth * scale);
        int scaleHeight = (int) Math.ceil(origHeight * scale);
        double ratio = (double) scaleWidth / scaleHeight;
        int resultWidth;
        int resultHeight;
        if (ratio <= PREF_RATIO) {
            // x-padding
            resultWidth = (int) Math.ceil(scaleHeight * PREF_RATIO);
            resultHeight = scaleHeight;
        } else {
            // y-padding
            resultWidth = scaleWidth;
            resultHeight = (int) Math.ceil(scaleWidth / PREF_RATIO);
        }
        return new ScalePad(
            scaleWidth, scaleHeight,
            resultWidth, resultHeight
        );
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
