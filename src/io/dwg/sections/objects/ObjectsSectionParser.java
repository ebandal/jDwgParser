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

        boolean useSequentialParsing = false;

        if (handles != null && !handles.allHandles().isEmpty() && version.from(DwgVersion.R2007)) {
            byte[] raw = stream.rawBytes();
            long outOfRangeCount = 0;

            for (long h : handles.allHandles()) {
                var offset = handles.offsetFor(h);
                if (offset.isPresent()) {
                    long off = offset.get();
                    if (off < 0 || off >= raw.length) outOfRangeCount++;
                }
            }

            double invalidRatio = (double) outOfRangeCount / handles.allHandles().size();
            if (invalidRatio > 0.2) {
                useSequentialParsing = true;
            }
        }

        if (!useSequentialParsing && handles != null && !handles.allHandles().isEmpty()) {
            byte[] raw = stream.rawBytes();

            for (Map.Entry<Long, Long> entry : sortedHandleOffsets()) {
                long handle = entry.getKey();
                long offset = entry.getValue();

                if (offset < 0 || offset >= raw.length) {
                    continue;
                }

                try {
                    DwgObject obj = parseObjectAt(raw, (int) offset, version, handle);
                    if (obj != null) {
                        result.put(handle, obj);
                    }
                } catch (Exception e) {
                    // Silently skip failed objects
                }
            }
        } else {
            result = parseStreaming(stream, version);
        }

        return result;
    }

    private Map<Long, DwgObject> parseStreaming(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<Long, DwgObject> result = new HashMap<>();
        byte[] raw = stream.rawBytes();
        long nextHandle = 1;
        long bitOffset = 0;

        // FIX 1: create buffer once and seek per iteration;
        // ByteBuffer.wrap(raw, offset, ...) + new ByteBufferBitInput(buf) is broken because
        // the constructor calls buffer.position(0), discarding the wrap() offset every time.
        ByteBufferBitInput bbuf = new ByteBufferBitInput(raw);

        while (bitOffset < (long)(raw.length - 6) * 8L) {
            long startBitOffset = bitOffset;
            try {
                bbuf.seek(bitOffset);
                BitStreamReader r = new BitStreamReader(bbuf, version);

                int objSizeBits = r.readModularShort();

                // R2010+: UMC (handlestream_size) between MS and type code
                if (version.from(DwgVersion.R2010)) {
                    r.readUMC();
                }

                long afterMsBitPos = bbuf.position();

                if (objSizeBits <= 0 || objSizeBits > 0x200000) {
                    bitOffset = startBitOffset + 16;
                    continue;
                }

                // R2010+ uses BOT; pre-R2010 uses BS
                int typeCode = version.from(DwgVersion.R2010) ? r.readBOT() : r.readBitShort();

                if (typeCode < 0 || typeCode > 5000) {
                    bitOffset = startBitOffset + 16;
                    continue;
                }

                DwgObject obj = createObject(typeCode);
                if (obj == null) {
                    // FIX 3: advance using bit positions (objSizeBits is in bits, not bytes)
                    bitOffset = afterMsBitPos + objSizeBits;
                    continue;
                }

                ((AbstractDwgObject) obj).setHandle(nextHandle);
                ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

                boolean skipHeaderStreaming = isSkipHeaderType(typeCode);
                if (!skipHeaderStreaming) {
                    try {
                        parseCommonHeader(r, obj, version);
                    } catch (Exception e) {
                        // Common header parsing failed, object still stored
                    }
                }
                resolver.resolve(typeCode).ifPresent(reader -> {
                    try {
                        reader.read(obj, r, version);
                    } catch (Exception e) {
                        // Type-specific parsing failed silently
                    }
                });

                result.put(nextHandle, obj);
                nextHandle++;

                // FIX 3: objSizeBits is the object size in bits starting from afterMsBitPos
                bitOffset = afterMsBitPos + objSizeBits;

            } catch (Exception e) {
                bitOffset = startBitOffset + 16;
            }
        }

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

        int objSize = r.readModularShort();

        if (objSize <= 0) {
            return null;
        }

        // R2010+: UMC (handlestream_size) comes between MS and type code
        if (version.from(DwgVersion.R2010)) {
            r.readUMC(); // skip handlestream_size
        }

        // R2010+ uses BOT (Bit Object Type); pre-R2010 uses BS
        int typeCode = version.from(DwgVersion.R2010) ? r.readBOT() : r.readBitShort();

        DwgObject obj = createObject(typeCode);
        if (obj == null) {
            return null;
        }

        ((AbstractDwgObject) obj).setHandle(handle);
        ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

        boolean skipHeader = isSkipHeaderType(typeCode);
        if (!skipHeader) {
            try {
                parseCommonHeader(r, obj, version);
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("Invalid BL opcode")) {
                    // Expected for certain types, continue without header
                } else {
                    throw e;
                }
            }
        }

        resolver.resolve(typeCode).ifPresent(reader -> {
            try {
                reader.read(obj, r, version);
            } catch (Exception e) {
                // Type-specific parsing failed silently
            }
        });

        return obj;
    }

    private void parseCommonHeader(BitStreamReader r, DwgObject obj, DwgVersion version)
            throws Exception {
        AbstractDwgObject ao = (AbstractDwgObject) obj;

        int numReactors = r.readBitLong();

        if (numReactors > 100000) {
            numReactors = 0;
        }

        boolean hasXDic = false;
        if (version.from(DwgVersion.R2004)) {
            hasXDic = r.getInput().readBit();
        }

        if (obj.isEntity() && obj instanceof AbstractDwgEntity) {
            AbstractDwgEntity ae = (AbstractDwgEntity) obj;
            int entityMode = r.getInput().readBits(2);
            ae.setEntityMode(entityMode);

            if (version.from(DwgVersion.R2000)) {
                int ltFlags = r.getInput().readBits(2);
                if (ltFlags == 3) {
                    // plotStyleFlags
                }
            }
        }

        ao.setOwnerHandle(new DwgHandleRef(r.readHandle()));

        for (int i = 0; i < numReactors; i++) {
            ao.addReactorHandle(new DwgHandleRef(r.readHandle()));
        }

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
        if (typeCode == 0x2A) {  // DICTIONARY
            return true;
        }
        if (typeCode == 0x35 || typeCode == 0x43 || typeCode == 0x45 ||
            typeCode == 0x49 || typeCode == 0x62 ||
            typeCode == 0x4F ||
            typeCode == 0x42 || typeCode == 0x44 || typeCode == 0x46) {
            return true;
        }
        if (typeCode >= 0x01 && typeCode <= 0x31) {
            return true;
        }
        if (typeCode == 0x3E || typeCode == 0x4B || typeCode == 0x4C ||
            typeCode == 0x51 || typeCode == 0x52 ||
            typeCode == 0x54 || typeCode == 0x55 || typeCode == 0x56 ||
            typeCode == 0x5A) {
            return true;
        }
        return false;
    }

    @Override
    public String sectionName() {
        return SectionType.OBJECTS.sectionName();
    }
}
