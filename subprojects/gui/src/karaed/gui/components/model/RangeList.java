package karaed.gui.components.model;

import karaed.engine.formats.ranges.RangeLike;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

final class RangeList<R extends RangeLike> {

    private final TreeMap<Integer, R> areas = new TreeMap<>();

    void clear() {
        areas.clear();
    }

    void add(R area) {
        this.areas.put(area.from(), area);
    }

    void addAll(Iterable<R> areas) {
        for (R area : areas) {
            add(area);
        }
    }

    boolean remove(R area) {
        return areas.entrySet().removeIf(e -> e.getValue() == area);
    }

    R before(R area) {
        Map.Entry<Integer, R> before = areas.lowerEntry(area.from()); // < from
        if (before == null)
            return null;
        return before.getValue();
    }

    R after(R area) {
        Map.Entry<Integer, R> after = areas.higherEntry(area.from()); // > from
        if (after == null)
            return null;
        return after.getValue();
    }

    boolean intersects(R newArea) {
        Map.Entry<Integer, R> before = areas.floorEntry(newArea.from()); // <= from
        if (before != null) {
            R areaBefore = before.getValue();
            if (areaBefore.contains(newArea.from()))
                return true;
        }
        Map.Entry<Integer, R> after = areas.higherEntry(newArea.from()); // > from
        if (after != null) {
            R areaAfter = after.getValue();
            if (newArea.contains(areaAfter.from()))
                return true;
        }
        return false;
    }

    R findContaining(int frame) {
        Map.Entry<Integer, R> floor = areas.floorEntry(frame); // <= frame
        if (floor == null)
            return null;
        R area = floor.getValue();
        if (area.contains(frame))
            return area;
        return null;
    }

    RangeSide isOnBorder(int frame, int delta, R[] area) {
        R areaBefore;
        int dx1;
        int dx2;
        Map.Entry<Integer, R> before = areas.floorEntry(frame); // <= frame
        if (before != null) {
            areaBefore = before.getValue();
            dx1 = Math.abs(areaBefore.from() - frame);
            dx2 = Math.abs(areaBefore.to() - frame);
        } else {
            areaBefore = null;
            dx1 = Integer.MAX_VALUE;
            dx2 = Integer.MAX_VALUE;
        }
        R areaAfter;
        int dx3;
        Map.Entry<Integer, R> after = areas.higherEntry(frame); // > frame
        if (after != null) {
            areaAfter = after.getValue();
            dx3 = Math.abs(areaAfter.from() - frame);
        } else {
            areaAfter = null;
            dx3 = Integer.MAX_VALUE;
        }
        if (dx1 <= dx2 && dx1 <= dx3) {
            if (dx1 < delta) {
                if (area != null) {
                    area[0] = areaBefore;
                }
                return RangeSide.LEFT;
            }
        } else if (dx2 <= dx1 && dx2 <= dx3) {
            if (dx2 < delta) {
                if (area != null) {
                    area[0] = areaBefore;
                }
                return RangeSide.RIGHT;
            }
        } else {
            if (dx3 < delta) {
                if (area != null) {
                    area[0] = areaAfter;
                }
                return RangeSide.LEFT;
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
