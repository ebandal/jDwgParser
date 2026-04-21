package io.dwg.sections.objects;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.*;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;
import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.handles.HandleRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * 스펙 §20 AcDb:AcDbObjects 섹션 파서.
 */
public class ObjectsSectionParser extends AbstractSectionParser<Map<Long, DwgObject>> {

    private HandleRegistry handles;
    private DwgClassRegistry classRegistry;
    private ObjectTypeResolver resolver;

    public ObjectsSectionParser() {
        this.resolver = ObjectTypeResolver.defaultResolver(new DwgClassRegistry());
    }

    public void setHandleRegistry(HandleRegistry handles) { this.handles = handles; }
    public void setClassRegistry(DwgClassRegistry classRegistry) {
        this.classRegistry = classRegistry;
        this.resolver = ObjectTypeResolver.defaultResolver(this.classRegistry);
    }

    @Override
    public Map<Long, DwgObject> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<Long, DwgObject> result = new HashMap<>();

        System.out.printf("[DEBUG] Objects: version=%s, handles=%d\n",
            version, handles != null ? handles.allHandles().size() : 0);

        // All DWG versions (R13~R2018) use offset-based parsing when handle registry is available
        // Objects section starts at offset 0 with no header
        if (handles != null && !handles.allHandles().isEmpty()) {
            byte[] raw = stream.rawBytes();
            System.out.printf("[DEBUG] Objects: Offset-based parsing, %d handles, section size=%d bytes\n",
                handles.allHandles().size(), raw.length);

            int successCount = 0;
            int failureCount = 0;

            // Parse objects using handle offsets (all offsets are absolute byte positions in Objects section)
            for (Map.Entry<Long, Long> entry : sortedHandleOffsets()) {
                long handle = entry.getKey();
                long offset = entry.getValue();

                if (offset < 0 || offset >= raw.length) {
                    System.out.printf("[DEBUG]   Handle 0x%X: offset %d out of range [0, %d)\n",
                        handle, offset, raw.length);
                    failureCount++;
                    continue;
                }

                try {
                    DwgObject obj = parseObjectAt(raw, (int) offset, version, handle);
                    if (obj != null) {
                        result.put(handle, obj);
                        successCount++;
                        if (successCount <= 5 || successCount % 100 == 0) {
                            System.out.printf("[DEBUG]   Handle 0x%X at offset 0x%X: SUCCESS\n",
                                handle, offset);
                        }
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    System.out.printf("[DEBUG]   Handle 0x%X at offset 0x%X: Exception %s\n",
                        handle, offset, e.getMessage());
                    failureCount++;
                }
            }

            System.out.printf("[DEBUG] Objects: Offset-based parse complete: %d/%d success, %d failed\n",
                successCount, handles.allHandles().size(), failureCount);
        } else {
            // No handle registry: use streaming parse (R2000+ without working Handles)
            System.out.printf("[DEBUG] Objects: No handle registry, using streaming parse for %s\n", version);
            result = parseStreaming(stream, version);
        }

        System.out.printf("[DEBUG] Objects: Total objects parsed=%d\n", result.size());
        return result;
    }

    private Map<Long, DwgObject> parseStreaming(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<Long, DwgObject> result = new HashMap<>();
        byte[] raw = stream.rawBytes();
        long nextHandle = 1;
        int offset = 0;  // Objects section starts at offset 0 (no header)
        int parsedCount = 0;
        int attemptCount = 0;

        System.out.printf("[DEBUG] Streaming parse: section size=%d bytes, starting at offset 0\n", raw.length);
        System.out.printf("[DEBUG] First 16 bytes from offset 0x%X: ", offset);
        for (int i = offset; i < Math.min(offset + 16, raw.length); i++) {
            System.out.printf("%02X ", raw[i] & 0xFF);
        }
        System.out.println();

        while (offset < raw.length - 6 && attemptCount < 50) {
            attemptCount++;
            try {
                // Create reader at current offset (start at byte boundary)
                ByteBufferBitInput bbuf = new ByteBufferBitInput(
                    java.nio.ByteBuffer.wrap(raw, offset, raw.length - offset));
                BitStreamReader r = new BitStreamReader(bbuf, version);

                int objSize = r.readModularShort();
                int unknownField = r.getInput().readRawShort() & 0xFFFF;

                if (attemptCount <= 5) {
                    System.out.printf("[DEBUG] Offset 0x%X: size=%d unknown=0x%04X\n",
                        offset, objSize, unknownField);
                }

                // Sanity check
                if (objSize <= 0 || objSize > 0x100000) {
                    offset += 2;
                    continue;
                }

                // Read type code (after skipping unknown field)
                int typeCode = r.readBitShort();

                // Sanity check - relax bounds to find valid objects
                if (typeCode < 0 || typeCode > 5000) {
                    offset += 1;
                    continue;
                }

                // Create object
                DwgObject obj = createObject(typeCode);
                if (obj == null) {
                    offset += 2;
                    continue;
                }

                // Set object properties
                ((AbstractDwgObject) obj).setHandle(nextHandle);
                ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

                // Parse common header and type-specific data
                try {
                    parseCommonHeader(r, obj, version);
                    resolver.resolve(typeCode).ifPresent(reader -> {
                        try {
                            reader.read(obj, r, version);
                        } catch (Exception e) {
                            // Type-specific parsing failed
                        }
                    });
                } catch (Exception e) {
                    // Common header parsing failed, but object is still valid
                }

                // Store object
                result.put(nextHandle, obj);
                parsedCount++;
                nextHandle++;

                // Move to next object (size field was 2 bytes)
                offset += 2 + objSize;

                if (parsedCount % 50 == 0) {
                    System.out.printf("[DEBUG] Streaming: Parsed %d objects, offset=0x%X\n", parsedCount, offset);
                }

            } catch (Exception e) {
                // Error at this offset, skip and continue
                offset += 2;
            }
        }

        System.out.printf("[DEBUG] Streaming parse complete: %d objects parsed\n", parsedCount);
        return result;
    }

    private Iterable<Map.Entry<Long, Long>> sortedHandleOffsets() {
        Map<Long, Long> map = new HashMap<>();
        for (long h : handles.allHandles()) {
            handles.offsetFor(h).ifPresent(o -> map.put(h, o));
        }
        return map.entrySet();
    }

    private DwgObject parseObjectAt(byte[] raw, int byteOffset, DwgVersion version, long handle)
            throws Exception {
        ByteBufferBitInput buf = new ByteBufferBitInput(
            java.nio.ByteBuffer.wrap(raw, byteOffset, raw.length - byteOffset));
        BitStreamReader r = new BitStreamReader(buf, version);

        // 객체 크기 (MS)
        int objSize = r.readModularShort();
        if (objSize <= 0) return null;

        // 타입 코드 (BS)
        int typeCode = r.readBitShort();

        DwgObject obj = createObject(typeCode);
        if (obj == null) return null;

        ((AbstractDwgObject) obj).setHandle(handle);
        ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

        // 공통 헤더 파싱
        parseCommonHeader(r, obj, version);

        // 타입별 파싱
        resolver.resolve(typeCode).ifPresent(reader -> {
            try {
                reader.read(obj, r, version);
            } catch (Exception e) {
                // 타입별 파싱 실패 무시
            }
        });

        return obj;
    }

    private void parseCommonHeader(BitStreamReader r, DwgObject obj, DwgVersion version)
            throws Exception {
        AbstractDwgObject ao = (AbstractDwgObject) obj;

        // numReactors (BL)
        int numReactors = r.readBitLong();

        // isXDic (B) - R2004+
        boolean hasXDic = false;
        if (version.from(DwgVersion.R2004)) {
            hasXDic = r.getInput().readBit();
        }

        // 엔티티 공통 헤더
        if (obj.isEntity() && obj instanceof AbstractDwgEntity) {
            AbstractDwgEntity ae = (AbstractDwgEntity) obj;
            int entityMode = r.getInput().readBits(2);
            ae.setEntityMode(entityMode);

            if (version.from(DwgVersion.R2000)) {
                int ltFlags = r.getInput().readBits(2);
                // lineType flags
                if (ltFlags == 3) {
                    // plotStyleFlags
                }
            }
        }

        // owner handle (H)
        ao.setOwnerHandle(new DwgHandleRef(r.readHandle()));

        // reactor handles
        for (int i = 0; i < numReactors; i++) {
            ao.addReactorHandle(new DwgHandleRef(r.readHandle()));
        }

        // xDic handle
        if (hasXDic) {
            ao.setXDicHandle(new DwgHandleRef(r.readHandle()));
        }
    }

    private DwgObject createObject(int typeCode) {
        DwgObjectType type = DwgObjectType.fromCode(typeCode);
        return switch (type) {
            case TEXT                -> new DwgText();
            case ATTDEF              -> new DwgAttdef();
            case ATTRIB              -> new DwgAttrib();
            case SEQEND              -> new DwgSeqEnd();
            case INSERT              -> new DwgInsert();
            case MINSERT             -> new DwgMinsert();
            case VERTEX_2D           -> new DwgVertex2D();
            case VERTEX_3D           -> new DwgVertex3D();
            case VERTEX_MESH         -> new DwgVertexMesh();
            case VERTEX_PFACE        -> new DwgVertexPface();
            case VERTEX_PFACE_FACE   -> new DwgVertexPfaceFace();
            case POLYLINE_2D         -> new DwgPolyline2D();
            case POLYLINE_3D         -> new DwgPolyline3D();
            case ARC                 -> new DwgArc();
            case CIRCLE              -> new DwgCircle();
            case LINE                -> new DwgLine();
            case DIMENSION_ORDINATE  -> new DwgDimensionOrdinate();
            case DIMENSION_LINEAR    -> new DwgDimensionLinear();
            case DIMENSION_ALIGNED   -> new DwgDimensionAligned();
            case DIMENSION_ANG_3PT   -> new DwgDimensionAng3pt();
            case DIMENSION_ANG_2LN   -> new DwgDimensionAng2ln();
            case DIMENSION_RADIUS    -> new DwgDimensionRadius();
            case DIMENSION_DIAMETER  -> new DwgDimensionDiameter();
            case POINT               -> new DwgPoint();
            case FACE3D              -> new DwgFace3D();
            case POLYLINE_PFACE      -> new DwgPolylinePface();
            case POLYLINE_MESH       -> new DwgPolylineMesh();
            case SOLID               -> new DwgSolid();
            case TRACE               -> new DwgTrace();
            case SHAPE               -> new DwgShape();
            case VIEWPORT            -> new DwgViewport();
            case ELLIPSE             -> new DwgEllipse();
            case SPLINE              -> new DwgSpline();
            case REGION              -> new DwgRegion();
            case SOLID3D             -> new DwgSolid3d();
            case BODY                -> new DwgBody();
            case RAY                 -> new DwgRay();
            case XLINE               -> new DwgXLine();
            case DICTIONARY          -> new DwgDictionary();
            case MTEXT               -> new DwgMText();
            case LEADER              -> new DwgLeader();
            case TOLERANCE           -> new DwgTolerance();
            case MLINE               -> new DwgMLine();
            case BLOCK_HEADER        -> new DwgBlockHeader();
            case BLOCK_END           -> new DwgBlockEnd();
            case LAYER               -> new DwgLayer();
            case GROUP               -> new DwgGroup();
            case OLE2FRAME           -> new DwgOle2frame();
            case LWPLINE             -> new DwgLwPolyline();
            case HATCH               -> new DwgHatch();
            case XRECORD             -> new DwgXrecord();
            case LTYPE               -> new DwgLtype();
            case STYLE               -> new DwgStyle();
            case VIEW                -> new DwgView();
            case UCS                 -> new DwgUcs();
            case VPORT               -> new DwgVport();
            case APPID               -> new DwgAppId();
            case DIMSTYLE            -> new DwgDimStyle();
            case MLINESTYLE          -> new DwgMLineStyle();
            case LONG_TRANSACTION    -> new DwgLongTransaction();
            case LAYOUT              -> new DwgLayout();
            default -> null;
        };
    }

    @Override
    public String sectionName() {
        return SectionType.OBJECTS.sectionName();
    }
}
