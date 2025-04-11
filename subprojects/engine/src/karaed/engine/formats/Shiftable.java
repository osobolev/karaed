package karaed.engine.formats;

import java.util.ArrayList;
import java.util.List;

public interface Shiftable<T extends Shiftable<T>> {

    T shift(double shift);

    static <T extends Shiftable<T>> List<T> shiftList(List<T> list, double shift) {
        List<T> newList = new ArrayList<>(list.size());
        for (T item : list) {
            newList.add(item.shift(shift));
        }
        return newList;
    }
}
