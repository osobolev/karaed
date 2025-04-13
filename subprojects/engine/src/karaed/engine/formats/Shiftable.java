package karaed.engine.formats;

import java.util.ArrayList;
import java.util.List;

public interface Shiftable<T extends Shiftable<T>> {

    T shift(double shift);

    static Double shift(Double value, double shift) {
        return value == null ? null : value.doubleValue() + shift;
    }

    static <T extends Shiftable<T>> List<T> shiftList(List<T> list, double shift) {
        List<T> newList = new ArrayList<>(list.size());
        for (T item : list) {
            newList.add(item.shift(shift));
        }
        return newList;
    }
}
