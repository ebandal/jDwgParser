package io.dwg.api;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import io.dwg.entities.DwgObject;

/**
 * The objects of a document by handle: a read-only map kept as a sorted array of primitive
 * handles and an array of objects (a HashMap of boxed handles took about 60 bytes more per
 * object). Iteration is in handle order.
 */
final class ObjectsByHandle extends AbstractMap<Long, DwgObject> {
    private final long[] handles;
    private final DwgObject[] objects;

    ObjectsByHandle(Map<Long, DwgObject> map) {
        long[] keys = new long[map.size()];
        int n = 0;
        for (Long k : map.keySet()) {
            keys[n++] = k;
        }
        Arrays.sort(keys);
        handles = keys;
        objects = new DwgObject[keys.length];
        for (int i = 0; i < keys.length; i++) {
            objects[i] = map.get(keys[i]);
        }
    }

    private int indexOf(Object key) {
        return key instanceof Long ? Arrays.binarySearch(handles, (Long) key) : -1;
    }

    @Override
    public DwgObject get(Object key) {
        int i = indexOf(key);
        return i >= 0 ? objects[i] : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return indexOf(key) >= 0;
    }

    @Override
    public int size() {
        return handles.length;
    }

    @Override
    public Collection<DwgObject> values() {
        return new AbstractCollection<DwgObject>() {
            @Override
            public Iterator<DwgObject> iterator() {
                return Arrays.asList(objects).iterator();
            }

            @Override
            public int size() {
                return objects.length;
            }
        };
    }

    @Override
    public Set<Entry<Long, DwgObject>> entrySet() {
        return new AbstractSet<Entry<Long, DwgObject>>() {
            @Override
            public Iterator<Entry<Long, DwgObject>> iterator() {
                return new Iterator<Entry<Long, DwgObject>>() {
                    private int i;

                    @Override
                    public boolean hasNext() {
                        return i < handles.length;
                    }

                    @Override
                    public Entry<Long, DwgObject> next() {
                        if (i >= handles.length) {
                            throw new NoSuchElementException();
                        }
                        Entry<Long, DwgObject> e = new SimpleImmutableEntry<>(handles[i], objects[i]);
                        i++;
                        return e;
                    }
                };
            }

            @Override
            public int size() {
                return handles.length;
            }
        };
    }
}
