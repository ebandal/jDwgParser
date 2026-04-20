package decode.section;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import decode.reader.ObjectReaderRegistry;
import structure.DwgVersion;
import structure.entities.*;

public final class DecoderObjects {
    private static final Logger log = Logger.getLogger(DecoderObjects.class.getName());

    public static class ObjectStream {
        public int objectSize;
        public int typeCode;
        public byte[] data;

        @Override
        public String toString() {
            return String.format("Object[type=0x%X, size=%d]", typeCode, objectSize);
        }
    }

    public static Map<Long, DwgObject> readObjects(byte[] buf, int off, DwgVersion ver,
                                                     Map<Long, Long> handleMap) {
        Map<Long, DwgObject> objects = new HashMap<>();

        // If we have a handle map (R13/R14), use offset-based reading
        if (handleMap != null && !handleMap.isEmpty()) {
            for (Map.Entry<Long, Long> entry : handleMap.entrySet()) {
                long handle = entry.getKey();
                long offset = entry.getValue();

                if (offset >= buf.length) continue;

                try {
                    DwgObject obj = parseObjectAt(buf, (int) offset, handle, ver);
                    if (obj != null) {
                        objects.put(handle, obj);
                    }
                } catch (Exception e) {
                    log.fine("Failed to parse object at offset " + offset + ": " + e.getMessage());
                }
            }
        } else {
            // R2000+: streaming parse (no handle map)
            int offset = off;
            int count = 0;

            while (offset < buf.length - 4) {
                try {
                    ObjectStream stream = parseObjectStream(buf, offset);
                    if (stream == null || stream.objectSize <= 0) {
                        offset += 2;
                        continue;
                    }

                    DwgObject obj = instantiateObject(stream, DwgObjectType.fromCode(stream.typeCode), ver);
                    if (obj != null) {
                        objects.put((long) count, obj);
                        count++;
                    }

                    offset += stream.objectSize;
                } catch (Exception e) {
                    log.fine("Object parse failed at offset " + offset);
                    offset += 2;
                }
            }
        }

        log.info("Read " + objects.size() + " objects");
        return objects;
    }

    private static DwgObject parseObjectAt(byte[] buf, int offset, long handle, DwgVersion ver) {
        if (offset + 4 > buf.length) return null;

        ObjectStream stream = parseObjectStream(buf, offset);
        if (stream == null) return null;

        DwgObject obj = instantiateObject(stream, DwgObjectType.fromCode(stream.typeCode), ver);
        if (obj instanceof AbstractDwgObject) {
            ((AbstractDwgObject) obj).setHandle(handle);
            ((AbstractDwgObject) obj).setRawTypeCode(stream.typeCode);
        }
        return obj;
    }

    private static ObjectStream parseObjectStream(byte[] buf, int offset) {
        if (offset + 4 > buf.length) return null;

        ObjectStream stream = new ObjectStream();

        // Object size (MS - modular short)
        int sizeBytes = getModularShortSize(buf, offset);
        stream.objectSize = readModularShort(buf, offset);
        offset += sizeBytes;

        if (stream.objectSize <= 0 || offset + 2 > buf.length) {
            return null;
        }

        // Type code (BS - bit short)
        stream.typeCode = (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8);
        offset += 2;

        // Remaining data
        int dataSize = stream.objectSize - (offset - (offset - sizeBytes - 2));
        if (dataSize > 0 && offset + dataSize <= buf.length) {
            stream.data = new byte[dataSize];
            System.arraycopy(buf, offset, stream.data, 0, dataSize);
        }

        return stream;
    }

    private static DwgObject instantiateObject(ObjectStream stream, DwgObjectType type, DwgVersion ver) {
        DwgObject obj = null;

        switch (type) {
            case LINE:
                obj = new DwgLine();
                break;
            case CIRCLE:
                obj = new DwgCircle();
                break;
            case ARC:
                obj = new DwgArc();
                break;
            case TEXT:
                obj = new DwgText();
                break;
            case INSERT:
                obj = new DwgInsert();
                break;
            case LAYER:
                obj = new DwgLayer();
                break;
            case ELLIPSE:
                obj = new DwgEllipse();
                break;
            case POINT:
                obj = new DwgPoint();
                break;
            case RAY:
                obj = new DwgRay();
                break;
            case BLOCK_HEADER:
                obj = new DwgBlockHeader();
                break;
            case BLOCK_END:
                obj = new DwgBlockEnd();
                break;
            case LTYPE:
                obj = new DwgLtype();
                break;
            case STYLE:
                obj = new DwgStyle();
                break;
            case DICTIONARY:
                obj = new DwgDictionary();
                break;
            default:
                log.fine("Unhandled object type: " + type);
                obj = new DwgLayer(); // Fallback
                break;
        }

        // Populate fields using ObjectReader if available
        if (obj != null && stream.data != null && stream.data.length > 0) {
            ObjectReaderRegistry.readObject(obj, stream.data, 0, ver);
        }

        return obj;
    }

    private static int readModularShort(byte[] buf, int off) {
        if (off >= buf.length) return 0;
        byte b1 = buf[off];
        if ((b1 & 0x80) == 0) {
            return b1 & 0x7F;
        } else {
            if (off + 2 > buf.length) return 0;
            return (((b1 & 0x7F) << 8) | (buf[off + 1] & 0xFF));
        }
    }

    private static int getModularShortSize(byte[] buf, int off) {
        if (off >= buf.length) return 1;
        return ((buf[off] & 0x80) != 0) ? 2 : 1;
    }
}
