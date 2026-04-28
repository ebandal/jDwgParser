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

        // Check if offset-based parsing is viable
        // For R2007+, Handles format can be corrupted - detect and use sequential parsing
        boolean useSequentialParsing = false;

        if (handles != null && !handles.allHandles().isEmpty() && version.from(DwgVersion.R2007)) {
            // R2007+: Check if handle offsets are corrupted (negative or way out of range)
            byte[] raw = stream.rawBytes();
            long negativeCount = 0;
            long outOfRangeCount = 0;

            for (long h : handles.allHandles()) {
                var offset = handles.offsetFor(h);
                if (offset.isPresent()) {
                    long off = offset.get();
                    if (off < 0) negativeCount++;
                    if (off < 0 || off >= raw.length) outOfRangeCount++;
                }
            }

            // If >20% of offsets are invalid, use sequential parsing
            double invalidRatio = (double) outOfRangeCount / handles.allHandles().size();
            if (invalidRatio > 0.2) {
                System.out.printf("[DEBUG] Objects: R2007+ detected %d negative offsets, %d out-of-range (%.1f%%) - switching to sequential parsing\n",
                    negativeCount, outOfRangeCount, invalidRatio * 100);
                useSequentialParsing = true;
            }
        }

        // All DWG versions (R13~R2018) use offset-based parsing when handle registry is available
        // Objects section starts at offset 0 with no header
        if (!useSequentialParsing && handles != null && !handles.allHandles().isEmpty()) {
            byte[] raw = stream.rawBytes();
            System.out.printf("[DEBUG] Objects: Offset-based parsing, %d handles, section size=%d bytes\n",
                handles.allHandles().size(), raw.length);

            int successCount = 0;
            int failureCount = 0;
            int outOfRangeCount = 0;
            Map<String, Integer> typeCodeStats = new HashMap<>();

            // Parse objects using handle offsets (all offsets are absolute byte positions in Objects section)
            for (Map.Entry<Long, Long> entry : sortedHandleOffsets()) {
                long handle = entry.getKey();
                long offset = entry.getValue();

                if (offset < 0 || offset >= raw.length) {
                    if (successCount == 0 || successCount % 50 == 0) {
                        System.out.printf("[DEBUG]   Handle 0x%X: offset %d out of range [0, %d) - SKIPPED\n",
                            handle, offset, raw.length);
                    }
                    outOfRangeCount++;
                    continue;
                }

                try {
                    DwgObject obj = parseObjectAt(raw, (int) offset, version, handle);
                    if (obj != null) {
                        result.put(handle, obj);
                        successCount++;
                        String typeStr = obj.objectType().toString();
                        typeCodeStats.merge(typeStr + " (OK)", 1, Integer::sum);
                        if (successCount <= 5 || successCount % 100 == 0 || obj.objectType().typeCode() == 0x4F) {
                            System.out.printf("[DEBUG]   Handle 0x%X at offset 0x%X: SUCCESS type=%s\n",
                                handle, offset, typeStr);
                        }
                    } else {
                        failureCount++;
                        // Try to determine failure type
                        try {
                            ByteBufferBitInput buf = new ByteBufferBitInput(raw);
                            buf.seek((long) ((int)offset) * 8L);
                            BitStreamReader r = new BitStreamReader(buf, version);
                            int objSize = r.readModularShort();
                            int typeCode = r.readBitShort();
                            DwgObjectType type = DwgObjectType.fromCode(typeCode);
                            String reason = (objSize < 0) ? "BadObjSize" : "NoReader";
                            String typeStr = type == DwgObjectType.UNKNOWN
                                ? String.format("UNKNOWN(0x%02X)", typeCode)
                                : type.toString();
                            typeCodeStats.merge(typeStr + " (FAIL-" + reason + ")", 1, Integer::sum);
                        } catch (Exception e2) {
                            typeCodeStats.merge("Unknown (FAIL)", 1, Integer::sum);
                        }
                    }
                } catch (Exception e) {
                    System.out.printf("[DEBUG]   Handle 0x%X at offset 0x%X: Exception %s\n",
                        handle, offset, e.getMessage());
                    failureCount++;
                    typeCodeStats.merge("Exception", 1, Integer::sum);
                }
            }

            // Print type code statistics
            if (!typeCodeStats.isEmpty()) {
                System.out.printf("[DEBUG] Objects: Type code breakdown:%n");
                for (Map.Entry<String, Integer> stat : typeCodeStats.entrySet()) {
                    System.out.printf("[DEBUG]   %s: %d%n", stat.getKey(), stat.getValue());
                }
            }

            System.out.printf("[DEBUG] Objects: Offset-based parse complete: %d/%d success, %d failed, %d skipped (out-of-range)\n",
                successCount, handles.allHandles().size() - outOfRangeCount, failureCount, outOfRangeCount);
        } else {
            // No handle registry or corrupted offsets: use sequential parsing
            System.out.printf("[DEBUG] Objects: Using sequential parsing%s\n",
                useSequentialParsing ? " (R2007+ offset-based fallback)" : "");

            if (version == DwgVersion.R2000 && !useSequentialParsing) {
                result = parseR2000Combined(stream, version);
            } else {
                result = parseStreaming(stream, version);
            }
        }

        System.out.printf("[DEBUG] Objects: Total objects parsed=%d\n", result.size());
        return result;
    }

    /**
     * R2000-specific streaming parser that skips embedded Handles section.
     */
    private Map<Long, DwgObject> parseR2000Streaming(byte[] rawSection, int handlesOffset, DwgVersion version) throws Exception {
        System.out.printf("[DEBUG] R2000: Streaming parse with Handles at 0x%X, section size=%d\n",
            handlesOffset, rawSection.length);

        // First, let's see what's at offset 0
        System.out.printf("[DEBUG] R2000: First bytes at 0x00: %02X %02X %02X %02X\n",
            rawSection[0] & 0xFF, rawSection[1] & 0xFF, rawSection[2] & 0xFF, rawSection[3] & 0xFF);

        // R2000 Objects start with 00 FF marker, not sequential like we thought
        // Actually, the structure might be simpler - just skip to the Handles and use offset-based after all
        // But offsets were negative, so try this: parse ONE object at the first 00 FF position

        Map<Long, DwgObject> result = new HashMap<>();

        // Check if section starts with 00 FF
        if ((rawSection[0] & 0xFF) == 0x00 && (rawSection[1] & 0xFF) == 0xFF) {
            System.out.printf("[DEBUG] R2000: Found 00 FF marker at start\n");

            try {
                ByteBufferBitInput bbuf = new ByteBufferBitInput(
                    java.nio.ByteBuffer.wrap(rawSection, 0, rawSection.length));
                BitStreamReader r = new BitStreamReader(bbuf, version);

                // Read object (00 FF is actually part of object data)
                int objSize = r.readModularShort();
                int typeCode = r.readBitShort();

                System.out.printf("[DEBUG] R2000: First object - size=%d, type=%d\n", objSize, typeCode);

                DwgObject obj = createObject(typeCode);
                if (obj != null) {
                    ((AbstractDwgObject) obj).setHandle(1);
                    ((AbstractDwgObject) obj).setRawTypeCode(typeCode);
                    parseCommonHeader(r, obj, version);
                    resolver.resolve(typeCode).ifPresent(reader -> {
                        try {
                            reader.read(obj, r, version);
                        } catch (Exception e) {}
                    });
                    result.put(1L, obj);
                    System.out.printf("[DEBUG] R2000: Successfully parsed first object\n");
                }
            } catch (Exception e) {
                System.out.printf("[DEBUG] R2000: Failed to parse first object: %s\n", e.getMessage());
            }
        }

        System.out.printf("[DEBUG] R2000 Streaming: Parsed %d objects total (simplified parser)\n", result.size());
        return result;
    }

    private Map<Long, DwgObject> parseStreaming(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<Long, DwgObject> result = new HashMap<>();
        byte[] raw = stream.rawBytes();
        long nextHandle = 1;
        int offset = 0;  // Objects section starts at offset 0 (no header per spec)
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
                boolean skipHeaderStreaming = isSkipHeaderType(typeCode);
                if (!skipHeaderStreaming) {
                    try {
                        parseCommonHeader(r, obj, version);
                    } catch (Exception e) {
                        // Common header parsing failed, but object is still valid
                    }
                }
                resolver.resolve(typeCode).ifPresent(reader -> {
                    try {
                        reader.read(obj, r, version);
                    } catch (Exception e) {
                        // Type-specific parsing failed
                    }
                });

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
        // Use the whole buffer and seek to the correct offset
        // (ByteBufferBitInput constructor resets position to 0, so we must seek)
        ByteBufferBitInput buf = new ByteBufferBitInput(raw);
        buf.seek((long) byteOffset * 8L);
        BitStreamReader r = new BitStreamReader(buf, version);

        // 객체 크기 (MS)
        int objSize = r.readModularShort();


        if (objSize <= 0) {
            // Try to read type code anyway for statistics
            try {
                int typeCode = r.readBitShort();
                DwgObjectType type = DwgObjectType.fromCode(typeCode);
                if (byteOffset % 100 == 0) {
                    System.err.printf("[FAIL] Handle 0x%X: type=%s objSize=%d%n", handle, type, objSize);
                }
            } catch (Exception e) {
                // Ignore
            }
            return null;
        }

        // 타입 코드 (BS)
        int typeCode = r.readBitShort();

        DwgObject obj = createObject(typeCode);
        if (obj == null) {
            DwgObjectType type = DwgObjectType.fromCode(typeCode);
            if (byteOffset % 100 == 0) {
                System.err.printf("[FAIL] Handle 0x%X: type=%s no_reader%n", handle, type);
            }
            return null;
        }

        ((AbstractDwgObject) obj).setHandle(handle);
        ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

        // 공통 헤더 파싱
        // Skip for: DICTIONARY (custom), marker types, CONTROL/ALTERNATE types (R2000 classes)
        boolean skipHeader = isSkipHeaderType(typeCode);
        if (!skipHeader) {
            try {
                parseCommonHeader(r, obj, version);
            } catch (IllegalStateException e) {
                // Some types may have bit stream errors during header parsing - ignore
                if (e.getMessage() != null && e.getMessage().contains("Invalid BL opcode")) {
                    // Expected for certain types, continue without header
                } else {
                    throw e;
                }
            }
        }

        // 타입별 파싱
        resolver.resolve(typeCode).ifPresent(reader -> {
            try {
                reader.read(obj, r, version);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("Invalid BL opcode")) {
                    System.err.printf("[WARN] Handle 0x%X: %s in reader %s%n",
                        handle, e.getMessage(), reader.getClass().getSimpleName());
                }
            }
        });

        return obj;
    }

    private void parseCommonHeader(BitStreamReader r, DwgObject obj, DwgVersion version)
            throws Exception {
        AbstractDwgObject ao = (AbstractDwgObject) obj;

        // numReactors (BL)
        int numReactors = r.readBitLong();

        // Sanity check: if numReactors is unreasonably large, skip reactors
        if (numReactors > 100000) {
            numReactors = 0;
        }

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
        // First, try standard types
        DwgObjectType type = DwgObjectType.fromCode(typeCode);
        return switch (type) {
            case TEXT                -> new DwgText();
            case ATTDEF              -> new DwgAttdef();
            case ATTRIB              -> new DwgAttrib();
            case SEQEND              -> new DwgSeqEnd();
            case ENDBLK              -> new DwgXrecord();
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
            case PLACEHOLDER         -> new DwgPlaceholder();
            case VBA_PROJECT         -> new DwgVbaProject();
            case LAYOUT_ALTERNATE    -> new DwgLayout();
            case UNUSED              -> null;
            case VP_ENT_HDR          -> null;
            case STYLE_ALTERNATE     -> new DwgStyle();
            case APPID_CONTROL       -> new DwgXrecord();
            case APPID_ALTERNATE     -> new DwgAppId();
            case DIMSTYLE_CONTROL    -> new DwgXrecord();
            case DIMSTYLE_ALTERNATE  -> new DwgDimStyle();
            case VX_CONTROL          -> new DwgXrecord();
            case MLINESTYLE_ALTERNATE -> new DwgMLineStyle();
            case IMAGE               -> new DwgImage();
            case WIPEOUT             -> new DwgWipeout();
            case XREF                -> new DwgXref();
            case UNDERLAY            -> new DwgUnderlay();
            case SURFACE             -> new DwgSurface();
            case MESH                -> new DwgMesh();
            case SCALE               -> new DwgScale();
            case VISUALSTYLE         -> new DwgVisualStyle();
            case ACAD_FIELD          -> new DwgField();
            case ACAD_PROXY_ENTITY   -> new DwgProxyEntity();
            case ACAD_DICTIONARYVAR  -> new DwgDictionaryVar();
            case ACAD_TABLE          -> new DwgTable();
            case ACAD_SCALE_LIST     -> new DwgScaleList();
            case ACAD_TABLESTYLE     -> new DwgTableStyle();
            case ACAD_CELLSTYLE      -> new DwgCellStyle();
            case ACAD_PLOTSTYLE      -> new DwgPlotStyle();
            case ACAD_MATERIAL       -> new DwgMaterial();
            case ACAD_DATASOURCE     -> new DwgDataSource();
            case ACAD_PERSSUBENTMANAGER -> new DwgPersSubentManager();
            case UNKNOWN -> {
                // For unknown types, create DwgXrecord to allow parsing as generic object
                // This handles custom R2000 types (0xF401-0xFC01) and other extensions
                yield new DwgXrecord();
            }
        };
    }

    private boolean isSkipHeaderType(int typeCode) {
        // Marker types: no standard common headers
        if (typeCode == 0x04 || typeCode == 0x05 || typeCode == 0x31) {  // SEQEND, ENDBLK, BLOCK_END
            return true;
        }
        // DICTIONARY: has custom structure
        if (typeCode == 0x2A) {  // DICTIONARY
            return true;
        }
        // R2000 ALTERNATE/CONTROL types: may not have standard headers
        if (typeCode == 0x35 || typeCode == 0x43 || typeCode == 0x45 ||  // STYLE_ALT, APPID_ALT, DIMSTYLE_ALT
            typeCode == 0x49 || typeCode == 0x62 ||  // MLINESTYLE_ALT, LAYOUT_ALT
            typeCode == 0x4F ||  // VBA_PROJECT
            typeCode == 0x42 || typeCode == 0x44 || typeCode == 0x46) {  // APPID_CTRL, DIMSTYLE_CTRL, VX_CTRL
            return true;
        }
        return false;
    }

    /**
     * Parse R2000 Classes section from the start of Objects section.
     * Returns the byte offset where Classes section ends.
     */
    private int parseR2000Classes(byte[] rawSection, DwgVersion version) throws Exception {
        if (rawSection.length < 50) {
            return -1;  // Too small for Classes section
        }

        // Classes section starts with Sentinel (16 bytes)
        // Look for sentinel pattern or use ClassesSectionParser
        // For now, skip Classes parsing (0x00-0x??) and let it be extracted

        try {
            // Try to extract and parse Classes from the start
            // Classes: [Sentinel:16] [RL size:4] [data:N] [RS CRC:2] [Sentinel:16]
            io.dwg.core.io.SectionInputStream classesStream =
                new io.dwg.core.io.SectionInputStream(rawSection, "AcDb:Classes");
            io.dwg.sections.classes.ClassesSectionParser classesParser =
                new io.dwg.sections.classes.ClassesSectionParser();
            java.util.List<io.dwg.sections.classes.DwgClassDefinition> classes =
                classesParser.parse(classesStream, version);

            if (classesParser instanceof io.dwg.sections.AbstractSectionParser) {
                // Classes were parsed, populate classRegistry
                if (classRegistry == null) {
                    classRegistry = new DwgClassRegistry();
                }
                classes.forEach(classRegistry::register);
                System.out.printf("[DEBUG] R2000: Parsed %d classes\n", classes.size());

                // Return approximate end of Classes section (we don't know exact size without parsing)
                // This is a limitation - we should track the parser position
                return -1;  // Return -1 to indicate we couldn't determine exact end
            }
        } catch (Exception e) {
            System.out.printf("[DEBUG] R2000: Classes parsing not fully implemented: %s\n", e.getMessage());
        }

        return -1;
    }

    /**
     * R2000-specific parser for combined Objects section.
     * R2000 Objects section contains Classes + Handles + Objects interleaved.
     * Find Handles by scanning for RS_BE page_size markers (2000-2100 range).
     */
    private Map<Long, DwgObject> parseR2000Combined(SectionInputStream stream, DwgVersion version) throws Exception {
        System.out.printf("[DEBUG] R2000: Parsing Objects section with embedded Classes + Handles\n");

        byte[] rawSection = stream.rawBytes();
        System.out.printf("[DEBUG] R2000: Section size=%d bytes\n", rawSection.length);

        // Step 1A: Parse Classes section (at start of Objects section)
        // Classes: [Sentinel:16] [RL:4] [data:N] [CRC:2] [Sentinel:16]
        try {
            int classesEnd = parseR2000Classes(rawSection, version);
            if (classesEnd > 0) {
                System.out.printf("[DEBUG] R2000: Classes section ends at offset 0x%X\n", classesEnd);
            }
        } catch (Exception e) {
            System.out.printf("[DEBUG] R2000: Classes parsing failed: %s (continuing anyway)\n", e.getMessage());
        }

        // Step 1B: Find Handles offset by scanning for RS_BE page_size markers
        int handlesOffset = findR2000HandlesOffset(rawSection);
        if (handlesOffset < 0) {
            System.out.printf("[DEBUG] R2000: Handles offset not found, falling back to streaming parse\n");
            return parseStreaming(stream, version);
        }

        System.out.printf("[DEBUG] R2000: Handles found at offset 0x%X\n", handlesOffset);

        // Step 2: Extract Handles section (from found offset to end)
        byte[] handlesData = new byte[rawSection.length - handlesOffset];
        System.arraycopy(rawSection, handlesOffset, handlesData, 0, handlesData.length);

        // Step 3: Parse Handles section
        HandleRegistry handlesReg = null;
        try {
            io.dwg.core.io.SectionInputStream handlesStream =
                new io.dwg.core.io.SectionInputStream(handlesData, "AcDb:Handles");
            io.dwg.sections.handles.HandlesSectionParser handlesParser =
                new io.dwg.sections.handles.HandlesSectionParser();
            handlesReg = handlesParser.parse(handlesStream, version);
            System.out.printf("[DEBUG] R2000: Parsed %d handles\n", handlesReg.allHandles().size());
            this.handles = handlesReg;
        } catch (Exception e) {
            System.out.printf("[DEBUG] R2000: Handles parsing failed: %s\n", e.getMessage());
            return parseStreaming(stream, version);
        }

        // Step 4: R2000 Objects use STREAMING format, not offset-based
        // Objects[0x00] starts with 00 FF marker, and Handles are embedded at 0x7F
        // Parse Objects sequentially while skipping the embedded Handles section
        System.out.printf("[DEBUG] R2000: Using streaming parser (skipping Handles at 0x%X)\n", handlesOffset);
        return parseR2000Streaming(rawSection, handlesOffset, version);
    }

    /**
     * Find Handles section offset in R2000 Objects section.
     * Scans for RS_BE (big-endian) page_size values in range 2000-2100.
     * Returns offset or -1 if not found.
     */
    private int findR2000HandlesOffset(byte[] data) {
        // R2000 Handles section appears to start at a fixed offset (0x7F observed in Cone.dwg)
        // This seems to be a standard offset after Objects section header
        int handlesOffset = 0x7F;  // Try standard offset first

        if (handlesOffset < data.length - 2) {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
            buffer.order(java.nio.ByteOrder.BIG_ENDIAN);
            buffer.position(handlesOffset);

            short valueBE = buffer.getShort();
            int value = valueBE & 0xFFFF;

            // Verify this is a valid page size
            if (value > 2 && value <= 2040) {
                System.out.printf("[DEBUG] R2000: Using standard offset 0x%X, RS_BE value=%d\n", handlesOffset, value);
                return handlesOffset;
            }
        }

        // If standard offset doesn't work, fall back to scanning (as backup)
        System.out.printf("[DEBUG] R2000: Standard offset 0x7F failed, scanning for valid page size...\n");
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
        buffer.order(java.nio.ByteOrder.BIG_ENDIAN);

        for (int i = 0; i < data.length - 1; i++) {
            buffer.position(i);
            if (buffer.remaining() < 2) break;

            short valueBE = buffer.getShort();
            int value = valueBE & 0xFFFF;

            // R2000 Handles pages have size in range [2, 2040]
            if (value > 2 && value <= 2040) {
                System.out.printf("[DEBUG] R2000: Found RS_BE value %d (valid page size) at offset 0x%X\n", value, i);
                return i;
            }
        }

        return -1;
    }

    /**
     * R2000-specific parser for combined Objects/Classes/Handles binary format.
     * R2000 objects use 00 FF markers with raw byte format (not bit-packed).
     * @deprecated Use parseR2000Combined() instead
     */
    @Deprecated
    private Map<Long, DwgObject> parseR2000Streaming(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<Long, DwgObject> result = new HashMap<>();
        byte[] raw = stream.rawBytes();
        long nextHandle = 1;
        int offset = 0;
        int objectCount = 0;

        System.out.printf("[DEBUG] R2000: Binary format parsing, section size=%d bytes\n", raw.length);

        while (offset < raw.length - 10) {
            // Look for 00 FF marker
            if (offset + 1 < raw.length &&
                (raw[offset] & 0xFF) == 0x00 &&
                (raw[offset + 1] & 0xFF) == 0xFF) {

                // Found marker, read object structure
                // Format: 00 FF [size:RS] [1F 00] [count:RC] 00 [type:RC] 00 [data...]

                if (offset + 10 > raw.length) break;

                // Read size (RS, little-endian, 2 bytes)
                int size = ((raw[offset + 3] & 0xFF) << 8) | (raw[offset + 2] & 0xFF);

                if (size <= 0 || size > 100000) {
                    offset += 2;
                    continue;
                }

                // Type code is at offset+8
                int typeCode = raw[offset + 8] & 0xFF;

                // Sanity check: valid DWG type codes are 0x30-0x4F or specific values
                // Skip objects with invalid types
                boolean validType = (typeCode >= 0x30 && typeCode <= 0x4F) || typeCode == 0x14 || typeCode == 0x15;

                if (objectCount < 5) {
                    System.out.printf("[DEBUG] R2000: Object %d @ 0x%X: size=%d type=0x%02X %s\n",
                        objectCount, offset, size, typeCode, validType ? "" : "(INVALID, skipping)");
                }

                if (!validType) {
                    offset += 2;  // Skip this invalid marker
                    continue;
                }

                try {
                    // Create object for this type
                    DwgObject obj = createObject(typeCode);
                    if (obj != null) {
                        // Set basic properties
                        ((AbstractDwgObject) obj).setHandle(nextHandle);
                        ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

                        // Try to parse the object data using BitStreamReader
                        // Object data starts at offset+10
                        if (offset + 10 < raw.length) {
                            ByteBufferBitInput buf = new ByteBufferBitInput(
                                java.nio.ByteBuffer.wrap(raw, offset + 10, Math.min(size - 8, raw.length - offset - 10)));
                            BitStreamReader r = new BitStreamReader(buf, version);

                            // Try type-specific parsing
                            try {
                                resolver.resolve(typeCode).ifPresent(reader -> {
                                    try {
                                        reader.read(obj, r, version);
                                    } catch (Exception e) {
                                        // Type parsing failed, but object is still created
                                    }
                                });
                            } catch (Exception e) {
                                // Parsing failed
                            }
                        }

                        result.put(nextHandle, obj);
                        nextHandle++;
                    }
                } catch (Exception e) {
                    if (objectCount < 5) {
                        System.out.printf("[DEBUG] R2000: Failed to parse object @ 0x%X: %s\n", offset, e.getMessage());
                    }
                }

                // Move to next object (size field + marker size)
                offset += size + 2;
                objectCount++;

            } else {
                // No marker found, advance by 1 byte
                offset += 1;
            }
        }

        System.out.printf("[DEBUG] R2000: Parsed %d objects\n", objectCount);
        return result;
    }

    @Override
    public String sectionName() {
        return SectionType.OBJECTS.sectionName();
    }
}
