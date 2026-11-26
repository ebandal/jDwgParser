package io.dwg.entities;

/**
 * Extrusion directions as entities keep them: almost all are the default (0, 0, 1), which is
 * kept as one shared array instead of three doubles per entity. The entities hand out copies,
 * so the shared array is never changed.
 */
public final class Extrusions {

    private static final double[] DEFAULT = {0.0, 0.0, 1.0};

    private Extrusions() {
    }

    /** The array to keep for {@code v}: the shared default when {@code v} is (0, 0, 1). */
    public static double[] share(double[] v) {
        if (v != null && v.length == 3 && v[0] == 0.0 && v[1] == 0.0 && v[2] == 1.0) {
            return DEFAULT;
        }
        return v;
    }

    /** The default direction, shared (only to be kept, not changed). */
    public static double[] shared() {
        return DEFAULT;
    }

    /** A copy to hand out ({@code null} stays {@code null}). */
    public static double[] copy(double[] v) {
        return v == null ? null : v.clone();
    }
}
