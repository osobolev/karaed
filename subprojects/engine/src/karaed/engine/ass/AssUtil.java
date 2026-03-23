package karaed.engine.ass;

public final class AssUtil {

    public interface CharAt {

        char charAt(int index);

        default boolean isRealLetter(int index) {
            return Character.isLetterOrDigit(charAt(index));
        }
    }

    public static boolean isLetter(int index, CharAt charAt) {
        if (charAt.isRealLetter(index))
            return true;
        char ch = charAt.charAt(index);
        if (ch == '-' || ch == '\'') {
            return index > 0 && charAt.isRealLetter(index - 1);
        }
        return false;
    }

    public static boolean isLetter(String string, int index) {
        return isLetter(index, string::charAt);
    }

    public static void appendK(StringBuilder buf, String tag, double len) {
        long k = Math.round(len * 100);
        if (k <= 0)
            return;
        buf.append(String.format("{\\%s%s}", tag, k));
    }

    public static void appendK(StringBuilder buf, double len) {
        appendK(buf, "k", len);
    }
}
