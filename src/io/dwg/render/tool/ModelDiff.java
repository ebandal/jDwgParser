package io.dwg.render.tool;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.dwg.render.model.BlockHeader;
import io.dwg.render.model.Drawing;
import io.dwg.render.model.Layer;
import io.dwg.render.model.entity.Entity;
import io.dwg.render.model.entity.Insert;

/**
 * Lists what differs between two drawings that should be the same : layers, blocks and entities are matched
 * by handle and compared field by field.
 */
final class ModelDiff {
    private static final double TOLERANCE = 1e-9;
    /** not compared : older versions have no lineweight and write names in upper case */
    private static final java.util.Set<String> VERSION_DEPENDENT = java.util.Set.of("lineweight", "name");

    private ModelDiff() {
    }

    /**
     * @param out   receives one line per difference
     * @param limit maximum number of lines
     */
    static void diff(Drawing actual, Drawing expected, List<String> out, int limit) {
        for (Map.Entry<Long, Layer> e : expected.layers.entrySet()) {
            Layer a = actual.layers.get(e.getKey());
            String d = a == null ? "missing" : compare(a, e.getValue(), "");
            add(out, limit, d, "layer", e.getKey(), e.getValue().name);
        }
        for (Map.Entry<Long, BlockHeader> e : expected.blocks.entrySet()) {
            BlockHeader a = actual.blocks.get(e.getKey());
            String d = a == null ? "missing" : compare(a, e.getValue(), "");
            add(out, limit, d, "block", e.getKey(), e.getValue().name);
        }

        Map<Long, Entity> actualEntities = entities(actual);
        Map<Long, String> actualPlace = places(actual);
        Map<Long, String> expectedPlace = places(expected);
        for (Map.Entry<Long, Entity> e : entities(expected).entrySet()) {
            Entity x = e.getValue();
            Entity a = actualEntities.get(e.getKey());
            String d;
            if (a == null) {
                d = "missing";
            } else if (a.getClass() != x.getClass()) {
                d = "is a " + a.getClass().getSimpleName();
            } else {
                d = compare(a, x, "");
                if (d == null && !java.util.Objects.equals(actualPlace.get(e.getKey()), expectedPlace.get(e.getKey()))) {
                    d = "belongs to " + actualPlace.get(e.getKey()) + ", expected " + expectedPlace.get(e.getKey());
                }
            }
            add(out, limit, d, x.getClass().getSimpleName(), e.getKey(), null);
        }
        Map<Long, Entity> expectedEntities = entities(expected);
        for (Map.Entry<Long, Entity> e : actualEntities.entrySet()) {
            if (!expectedEntities.containsKey(e.getKey())) {
                add(out, limit, "the other drawing does not have it (" + e.getValue().object.typeName + ", "
                        + actualPlace.get(e.getKey()) + ")", e.getValue().getClass().getSimpleName(), e.getKey(), null);
            }
        }
    }

    private static void add(List<String> out, int limit, String difference, String kind, long handle, String name) {
        if (difference != null && out.size() < limit) {
            out.add(String.format("%s %X%s : %s", kind, handle, name == null ? "" : " '" + name + "'", difference));
        }
    }

    // every entity by handle, including attributes
    private static Map<Long, Entity> entities(Drawing d) {
        Map<Long, Entity> map = new LinkedHashMap<>();
        List<Entity> all = new ArrayList<>(d.modelSpace);
        d.blockEntities.values().forEach(all::addAll);
        for (Entity e : all) {
            map.put(e.object.handle, e);
            if (e instanceof Insert) {
                for (Entity a : ((Insert) e).attribs) {
                    map.put(a.object.handle, a);
                }
            }
        }
        return map;
    }

    // where each entity is : the model space or the block that owns it
    private static Map<Long, String> places(Drawing d) {
        Map<Long, String> map = new LinkedHashMap<>();
        for (Entity e : d.modelSpace) {
            map.put(e.object.handle, "model space");
        }
        d.blockEntities.forEach((block, list) -> {
            for (Entity e : list) {
                map.put(e.object.handle, "block " + Long.toHexString(block).toUpperCase());
            }
        });
        return map;
    }

    /** @return null if equal, else the first field that differs */
    private static String compare(Object a, Object x, String path) {
        if (a == null || x == null) {
            return a == x ? null : path + " " + show(x) + " != " + show(a);
        }
        Class<?> type = x.getClass();
        if (type != a.getClass()) {
            return path + " is a " + a.getClass().getSimpleName();
        }
        if (x instanceof Double) {
            double e = (Double) x;
            double v = (Double) a;
            return Math.abs(e - v) <= TOLERANCE * Math.max(1.0, Math.abs(e)) || (Double.isNaN(e) && Double.isNaN(v))
                    ? null : path + " " + e + " != " + v;
        }
        if (x instanceof Number || x instanceof Boolean || x instanceof String || x instanceof Character) {
            return x.equals(a) ? null : path + " " + show(x) + " != " + show(a);
        }
        if (type.isArray()) {
            int n = Array.getLength(x);
            if (n != Array.getLength(a)) {
                return path + " has " + Array.getLength(a) + " elements, expected " + n;
            }
            for (int i = 0; i < n; i++) {
                String d = compare(Array.get(a, i), Array.get(x, i), path + "[" + i + "]");
                if (d != null) {
                    return d;
                }
            }
            return null;
        }
        if (x instanceof Collection) {
            return compare(((Collection<?>) a).toArray(), ((Collection<?>) x).toArray(), path);
        }
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                // "object" is the bookkeeping of where the entity came from, not part of its value
                if (Modifier.isStatic(f.getModifiers()) || !Modifier.isPublic(f.getModifiers())
                        || f.getName().equals("object") || VERSION_DEPENDENT.contains(f.getName())) {
                    continue;
                }
                try {
                    String d = compare(f.get(a), f.get(x), path.isEmpty() ? f.getName() : path + "." + f.getName());
                    if (d != null) {
                        return d;
                    }
                } catch (IllegalAccessException e) {
                    // not comparable
                }
            }
        }
        return null;
    }

    private static String show(Object o) {
        String s = o instanceof String ? "'" + o + "'" : String.valueOf(o);
        s = s.replaceAll("\\p{Cntrl}", "?");
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }
}
