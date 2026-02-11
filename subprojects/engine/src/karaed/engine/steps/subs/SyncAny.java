package karaed.engine.steps.subs;

import karaed.engine.KaraException;

class SyncAny {

    static String getWhere(int iseg, String segment) {
        return segment == null ? "segment " + iseg : "'" + segment + "'";
    }

    private static double checkTimestamp(Double t, String key, int iseg, String segment, String entity, int index) {
        if (t == null || t.isNaN()) {
            String where = getWhere(iseg, segment);
            throw new KaraException(String.format(
                "Missing \"%s\" timestamp at %s, %s %s", key, where, entity, index
            ));
        }
        return t.doubleValue();
    }

    static Timestamps checkTimestamps(Double start, Double end, int iseg, String segment, String entity, int index) {
        double from = checkTimestamp(start, "start", iseg, segment, entity, index);
        double to = checkTimestamp(end, "end", iseg, segment, entity, index);
        return new Timestamps(from, to);
    }
}
