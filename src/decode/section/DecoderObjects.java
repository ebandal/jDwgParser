package decode.section;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
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
                                                     Map<Long, Long> handleMap, structure.Dwg dwg) {
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
            // R2000+: Sequential bit-level reading from entire section
            ByteBuffer bbuf = ByteBuffer.wrap(buf, off, buf.length - off).order(ByteOrder.LITTLE_ENDIAN);
            ByteBufferBitInput bitInput = new ByteBufferBitInput(bbuf);
            BitStreamReader reader = new BitStreamReader(bitInput, mapVersion(ver));
            int objectCount = 0;

            while (!bitInput.isEof() && objectCount < 10000) {
                try {
                    long startPos = reader.position();

                    // Read object size (MS)
                    int objectSize = reader.readModularShort();

                    if (objectSize <= 0 || objectSize > 100000) {
                        // Skip invalid record
                        continue;
                    }

                    // Read type code (BS)
                    int typeCode = reader.readBitShort();

                    // Create and populate object with fields read from BitStreamReader
                    long startFieldPos = reader.position();
                    DwgObject obj = instantiateAndReadObject(typeCode, ver, reader);
                    long endFieldPos = reader.position();

                    // Log field reading for debugging
                    if (typeCode >= 0x11 && typeCode <= 0x23) {
                        log.info(String.format("Entity 0x%X: read %d bits (%d bytes)",
                            typeCode, endFieldPos - startFieldPos, (endFieldPos - startFieldPos + 7) / 8));
                    }

                    // Skip CRC (BS = 16 bits)
                    reader.readBitShort();

                    if (obj != null) {
                        long globalCount = dwg != null ? dwg.globalObjectCounter.getAndIncrement() : 0;
                        objects.put(globalCount, obj);
                        objectCount++;
                    }

                } catch (Exception e) {
                    log.fine("Object parse failed: " + e.getMessage());
                    // Try to continue from next position
                    break;
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

        // According to spec: Object Record = Size (MS) + Type (BS) + Data + CRC (RS)
        // Size typically includes everything except the size field itself
        // So: Total = sizeBytes + 2(type) + dataSize + 2(CRC)
        // dataSize = objectSize - 2(type) - 2(CRC)
        // But objectSize might NOT include sizeBytes
        int dataSize = stream.objectSize - 2 - 2;

        // Safety check: dataSize should be non-negative and fit in buffer
        if (dataSize < 0) {
            // Try alternative: objectSize might include sizeBytes
            dataSize = stream.objectSize - sizeBytes - 2 - 2;
        }

        if (dataSize > 0 && offset + dataSize + 2 <= buf.length) {
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

    private static DwgObject instantiateAndReadObject(int typeCode, DwgVersion ver, BitStreamReader reader) {
        DwgObject obj = null;
        DwgObjectType type = DwgObjectType.fromCode(typeCode);

        try {
            switch (type) {
                case LINE:
                    obj = new DwgLine();
                    readLineFields((DwgLine) obj, reader);
                    break;
                case CIRCLE:
                    obj = new DwgCircle();
                    readCircleFields((DwgCircle) obj, reader);
                    break;
                case ARC:
                    obj = new DwgArc();
                    readArcFields((DwgArc) obj, reader);
                    break;
                case TEXT:
                    obj = new DwgText();
                    readTextFields((DwgText) obj, reader);
                    break;
                case INSERT:
                    obj = new DwgInsert();
                    readInsertFields((DwgInsert) obj, reader);
                    break;
                case LAYER:
                    obj = new DwgLayer();
                    readLayerFields((DwgLayer) obj, reader);
                    break;
                case ELLIPSE:
                    obj = new DwgEllipse();
                    readEllipseFields((DwgEllipse) obj, reader);
                    break;
                case POINT:
                    obj = new DwgPoint();
                    readPointFields((DwgPoint) obj, reader);
                    break;
                case RAY:
                    obj = new DwgRay();
                    readRayFields((DwgRay) obj, reader);
                    break;
                case BLOCK_HEADER:
                    obj = new DwgBlockHeader();
                    readBlockHeaderFields((DwgBlockHeader) obj, reader);
                    break;
                case BLOCK_END:
                    obj = new DwgBlockEnd();
                    readBlockEndFields((DwgBlockEnd) obj, reader);
                    break;
                case LTYPE:
                    obj = new DwgLtype();
                    readLtypeFields((DwgLtype) obj, reader);
                    break;
                case STYLE:
                    obj = new DwgStyle();
                    readStyleFields((DwgStyle) obj, reader);
                    break;
                case DICTIONARY:
                    obj = new DwgDictionary();
                    readDictionaryFields((DwgDictionary) obj, reader);
                    break;
                default:
                    log.fine("Unhandled object type: " + type);
                    obj = new DwgLayer();
                    readLayerFields((DwgLayer) obj, reader);
                    break;
            }
        } catch (Exception e) {
            log.warning("Failed to read object type 0x" + Integer.toHexString(typeCode) + ": " + e.getMessage());
        }

        return obj;
    }

    // Field reading methods - read fields from BitStreamReader
    private static void readLineFields(DwgLine obj, BitStreamReader r) throws Exception {
        // R2000+: Complex format for Line (not simple 3BD)
        boolean zAreZero = r.getInput().readBit();
        double sx = r.getInput().readRawDouble();
        double ex = r.getInput().readRawDouble();
        double sy = r.getInput().readRawDouble();
        double ey = r.getInput().readRawDouble();
        double sz = zAreZero ? 0.0 : r.getInput().readRawDouble();
        double ez = zAreZero ? 0.0 : r.getInput().readRawDouble();

        log.info(String.format("Line: start=(%.2f,%.2f,%.2f) end=(%.2f,%.2f,%.2f)",
            sx, sy, sz, ex, ey, ez));

        r.readBitThickness();
        r.readBitExtrusion();
    }

    private static void readCircleFields(DwgCircle obj, BitStreamReader r) throws Exception {
        double[] center = r.read3BitDouble();
        double radius = r.readBitDouble();
        r.readBitThickness();
        r.readBitExtrusion();

        log.info(String.format("Circle: center=(%.2f,%.2f,%.2f) radius=%.2f",
            center[0], center[1], center[2], radius));
    }

    private static void readArcFields(DwgArc obj, BitStreamReader r) throws Exception {
        double[] center = r.read3BitDouble();
        double radius = r.readBitDouble();
        r.readBitThickness();
        r.readBitExtrusion();
        double startAngle = r.readBitDouble();
        double endAngle = r.readBitDouble();

        log.info(String.format("Arc: center=(%.2f,%.2f,%.2f) radius=%.2f angle=[%.2f,%.2f]",
            center[0], center[1], center[2], radius, startAngle, endAngle));

        obj.setCenter(new structure.entities.Point3D(center[0], center[1], center[2]));
        obj.setRadius(radius);
        obj.setStartAngle(startAngle);
        obj.setEndAngle(endAngle);
    }

    private static void readEllipseFields(DwgEllipse obj, BitStreamReader r) throws Exception {
        double[] center = r.read3BitDouble();
        double[] major = r.read3BitDouble();
        double[] extrusion = r.readBitExtrusion();
        double ratio = r.readBitDouble();
        double startParam = r.readBitDouble();
        double endParam = r.readBitDouble();

        log.info(String.format("Ellipse: center=(%.2f,%.2f,%.2f) major=(%.2f,%.2f,%.2f) ratio=%.2f",
            center[0], center[1], center[2], major[0], major[1], major[2], ratio));

        obj.setCenter(new structure.entities.Point3D(center[0], center[1], center[2]));
        obj.setMajorAxisVec(new structure.entities.Point3D(major[0], major[1], major[2]));
        obj.setExtrusion(extrusion);
        obj.setAxisRatio(ratio);
        obj.setStartParam(startParam);
        obj.setEndParam(endParam);
    }

    private static void readTextFields(DwgText obj, BitStreamReader r) throws Exception {
        // Skip for now - read common fields if any
    }

    private static void readInsertFields(DwgInsert obj, BitStreamReader r) throws Exception {
        // Skip for now
    }

    private static void readLayerFields(DwgLayer obj, BitStreamReader r) throws Exception {
        // Skip for now
    }

    private static void readPointFields(DwgPoint obj, BitStreamReader r) throws Exception {
        double[] point = r.read3BitDouble();
        log.info(String.format("Point: (%.2f,%.2f,%.2f)", point[0], point[1], point[2]));
    }

    private static void readRayFields(DwgRay obj, BitStreamReader r) throws Exception {
        double[] point = r.read3BitDouble();
        double[] direction = r.read3BitDouble();
        log.info(String.format("Ray: point=(%.2f,%.2f,%.2f) dir=(%.2f,%.2f,%.2f)",
            point[0], point[1], point[2], direction[0], direction[1], direction[2]));
    }

    private static void readBlockHeaderFields(DwgBlockHeader obj, BitStreamReader r) throws Exception {
        // Skip for now
    }

    private static void readBlockEndFields(DwgBlockEnd obj, BitStreamReader r) throws Exception {
        // Skip for now
    }

    private static void readLtypeFields(DwgLtype obj, BitStreamReader r) throws Exception {
        // Skip for now
    }

    private static void readStyleFields(DwgStyle obj, BitStreamReader r) throws Exception {
        // Skip for now
    }

    private static void readDictionaryFields(DwgDictionary obj, BitStreamReader r) throws Exception {
        // Skip for now
    }

    private static io.dwg.core.version.DwgVersion mapVersion(DwgVersion version) {
        if (version == null) return io.dwg.core.version.DwgVersion.R2004;

        switch (version) {
            case R13: return io.dwg.core.version.DwgVersion.R13;
            case R14: return io.dwg.core.version.DwgVersion.R14;
            case R2000: return io.dwg.core.version.DwgVersion.R2000;
            case R2004: return io.dwg.core.version.DwgVersion.R2004;
            case R2007: return io.dwg.core.version.DwgVersion.R2007;
            case R2010: return io.dwg.core.version.DwgVersion.R2010;
            case R2013: return io.dwg.core.version.DwgVersion.R2013;
            case R2018: return io.dwg.core.version.DwgVersion.R2018;
            default: return io.dwg.core.version.DwgVersion.R2004;
        }
    }
}
