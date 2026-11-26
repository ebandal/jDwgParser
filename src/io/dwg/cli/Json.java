package io.dwg.cli;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Writes maps, lists, strings, numbers, booleans and null as JSON (RFC 8259). */
final class Json {

    private Json() {
    }

    static String write(Object value) {
        StringBuilder b = new StringBuilder();
        write(b, value, "");
        return b.toString();
    }

    private static void write(StringBuilder b, Object v, String indent) {
        if (v == null) {
            b.append("null");
        } else if (v instanceof String) {
            quote(b, (String) v);
        } else if (v instanceof Double || v instanceof Float) {
            double d = ((Number) v).doubleValue();
            b.append(Double.isFinite(d) ? (d == Math.rint(d) && Math.abs(d) < 1e15 ? Long.toString((long) d) : Double.toString(d)) : "null");
        } else if (v instanceof Number || v instanceof Boolean) {
            b.append(v);
        } else if (v instanceof Map) {
            String inner = indent + "  ";
            b.append('{');
            Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) v).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                b.append('\n').append(inner);
                quote(b, String.valueOf(e.getKey()));
                b.append(": ");
                write(b, e.getValue(), inner);
                if (it.hasNext()) b.append(',');
            }
            b.append('\n').append(indent).append('}');
        } else if (v instanceof List) {
            String inner = indent + "  ";
            List<?> list = (List<?>) v;
            b.append('[');
            for (int i = 0; i < list.size(); i++) {
                b.append('\n').append(inner);
                write(b, list.get(i), inner);
                if (i < list.size() - 1) b.append(',');
            }
            b.append(list.isEmpty() ? "]" : "\n" + indent + "]");
        } else {
            quote(b, v.toString());
        }
    }

    private static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }
}
