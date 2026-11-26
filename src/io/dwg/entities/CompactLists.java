package io.dwg.entities;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;

/**
 * Lists that keep their elements as primitive values: a drawing can have hundreds of thousands
 * of handles, points or numbers in lists (a block's owned entities, spline knots), and a boxed
 * element costs several times its value. They are ordinary {@code List}s to their users; an
 * element is made when it is read.
 */
public final class CompactLists {

    private CompactLists() {
    }

    /** Handles, kept as longs; null handles are kept as 0. */
    public static final class Handles extends AbstractList<DwgHandleRef> implements RandomAccess {
        private long[] values = new long[4];
        private int size;

        @Override
        public DwgHandleRef get(int index) {
            check(index, size);
            return values[index] == 0 ? DwgHandleRef.NULL : new DwgHandleRef(values[index]);
        }

        @Override
        public DwgHandleRef set(int index, DwgHandleRef ref) {
            DwgHandleRef old = get(index);
            values[index] = ref == null ? 0 : ref.rawHandle();
            return old;
        }

        @Override
        public boolean add(DwgHandleRef ref) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = ref == null ? 0 : ref.rawHandle();
            modCount++;
            return true;
        }

        @Override
        public int size() {
            return size;
        }
    }

    /** Numbers, kept as doubles; null is not allowed. */
    public static final class Doubles extends AbstractList<Double> implements RandomAccess {
        private double[] values = new double[4];
        private int size;

        @Override
        public Double get(int index) {
            check(index, size);
            return values[index];
        }

        @Override
        public Double set(int index, Double value) {
            Double old = get(index);
            values[index] = value;
            return old;
        }

        @Override
        public boolean add(Double value) {
            if (size == values.length) values = Arrays.copyOf(values, size * 2);
            values[size++] = value;
            modCount++;
            return true;
        }

        @Override
        public int size() {
            return size;
        }
    }

    /** Points, kept as three doubles each; null is not allowed. */
    public static final class Points extends AbstractList<Point3D> implements RandomAccess {
        private double[] values = new double[12];
        private int size;

        @Override
        public Point3D get(int index) {
            check(index, size);
            return new Point3D(values[3 * index], values[3 * index + 1], values[3 * index + 2]);
        }

        @Override
        public Point3D set(int index, Point3D p) {
            Point3D old = get(index);
            put(index, p);
            return old;
        }

        @Override
        public boolean add(Point3D p) {
            if (3 * size == values.length) values = Arrays.copyOf(values, values.length * 2);
            put(size++, p);
            modCount++;
            return true;
        }

        private void put(int index, Point3D p) {
            values[3 * index] = p.x();
            values[3 * index + 1] = p.y();
            values[3 * index + 2] = p.z();
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static void check(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + ", size " + size);
        }
    }
}
