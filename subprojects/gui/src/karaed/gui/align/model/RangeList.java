package karaed.gui.align.model;

import karaed.engine.formats.ranges.RangeLike;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.TreeMap;

final class RangeList<R extends RangeLike> {

    private final TreeMap<Integer, R> areas = new TreeMap<>();

    void add(R area) {
        this.areas.put(area.from(), area);
    }

    boolean remove(R area) {
        return areas.entrySet().removeIf(e -> e.getValue() == area);
    }

    boolean intersects(R newArea) {
        NavigableSet<Integer> keySet = areas.navigableKeySet();
        Integer before = keySet.floor(newArea.from()); // <= from
        if (before != null) {
            R areaBefore = areas.get(before);
            if (areaBefore.contains(newArea.from()))
                return true;
        }
        Integer after = keySet.higher(newArea.from()); // > from
        if (after != null) {
            R areaAfter = areas.get(after);
            if (newArea.contains(areaAfter.from()))
                return true;
        }
        return false;
    }

    R findArea(int frame) {
        Integer floor = areas.navigableKeySet().floor(frame);
        if (floor == null)
            return null;
        R area = areas.get(floor);
        if (area.contains(frame))
            return area;
        return null;
    }

    AreaSide isOnAreaBorder(int frame, int delta, R[] area) {
        NavigableSet<Integer> keySet = areas.navigableKeySet();
        R areaBefore;
        int dx1;
        int dx2;
        Integer before = keySet.floor(frame); // <= frame
        if (before != null) {
            areaBefore = areas.get(before);
            dx1 = Math.abs(before.intValue() - frame);
            dx2 = Math.abs(areaBefore.to() - frame);
        } else {
            areaBefore = null;
            dx1 = Integer.MAX_VALUE;
            dx2 = Integer.MAX_VALUE;
        }
        R areaAfter;
        int dx3;
        Integer after = keySet.higher(frame); // > frame
        if (after != null) {
            areaAfter = areas.get(after);
            dx3 = Math.abs(after.intValue() - frame);
        } else {
            areaAfter = null;
            dx3 = Integer.MAX_VALUE;
        }
        if (dx1 <= dx2 && dx1 <= dx3) {
            if (dx1 < delta) {
                if (area != null) {
                    area[0] = areaBefore;
                }
                return AreaSide.LEFT;
            }
        } else if (dx2 <= dx1 && dx2 <= dx3) {
            if (dx2 < delta) {
                if (area != null) {
                    area[0] = areaBefore;
                }
                return AreaSide.RIGHT;
            }
        } else {
            if (dx3 < delta) {
                if (area != null) {
                    area[0] = areaAfter;
                }
                return AreaSide.LEFT;
            }
        }
        return null;
    }

    int size() {
        return areas.size();
    }

    Collection<R> values() {
        return areas.values();
    }
}
