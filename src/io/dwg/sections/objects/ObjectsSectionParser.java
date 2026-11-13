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
import io.dwg.core.type.CmColor;

/**
 * 스펙 §20 AcDb:AcDbObjects 섹션 파서.
 */
public class ObjectsSectionParser extends AbstractSectionParser<Map<Long, DwgObject>> {

    private HandleRegistry handles;
    private DwgClassRegistry classRegistry;
    private ObjectTypeResolver resolver;
    private java.nio.charset.Charset textCharset;

    public ObjectsSectionParser() {
        this.resolver = ObjectTypeResolver.defaultResolver(new DwgClassRegistry());
    }

    public void setHandleRegistry(HandleRegistry handles) { this.handles = handles; }
    /** R2004 이하 파일의 문자열을 디코딩할 문자 집합 (도면의 코드페이지) */
    public void setTextCharset(java.nio.charset.Charset textCharset) { this.textCharset = textCharset; }
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

        // Create buffer once and seek per iteration — ByteBuffer.wrap() loses the offset
        // because the ByteBufferBitInput constructor calls buffer.position(0).
        ByteBufferBitInput bbuf = new ByteBufferBitInput(raw);

        while (bitOffset < (long)(raw.length - 6) * 8L) {
            long startBitOffset = bitOffset;
            try {
                bbuf.seek(bitOffset);
                BitStreamReader r = new BitStreamReader(bbuf, version);
                r.setTextCharset(textCharset);

                // MS returns object size in BYTES (libredwg: obj->size = bit_read_MS(dat); dat->size = obj->size)
                int objSizeBytes = r.readModularShort();

                // R2010+: UMC (handlestream_size in bits) comes between MS and type code.
                // obj->address = dat->byte after UMC; next object at (obj->address + obj->size).
                long handleStreamBits = -1;
                if (version.from(DwgVersion.R2010)) {
                    handleStreamBits = r.readUMC();
                }

                // Bit position of obj->address (start of object data, after MS+UMC)
                long objDataStartBit = bbuf.position();

                if (objSizeBytes <= 0 || objSizeBytes > 0x40000) {
                    bitOffset = startBitOffset + 16;
                    continue;
                }

                // R2010+ uses BOT; pre-R2010 uses BS
                int typeCode = version.from(DwgVersion.R2010) ? r.readBOT() : r.readBitShort();

                if (typeCode < 0 || typeCode > 5000) {
                    bitOffset = startBitOffset + 16;
                    continue;
                }

                DwgObjectType type = resolveType(typeCode);
                DwgObject obj = createObject(type);
                if (obj == null) {
                    // Next object starts at obj->address + obj->size (both in bytes → bits)
                    bitOffset = objDataStartBit + (long)objSizeBytes * 8L;
                    continue;
                }

                ((AbstractDwgObject) obj).setHandle(nextHandle);
                ((AbstractDwgObject) obj).setRawTypeCode(typeCode);
                readObjectData(obj, type, r, version, objDataStartBit, objSizeBytes, handleStreamBits);

                result.put(nextHandle, obj);
                nextHandle++;

                bitOffset = objDataStartBit + (long)objSizeBytes * 8L;

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
        ByteBufferBitInput buf = new ByteBufferBitInput(raw);
        buf.seek((long) byteOffset * 8L);
        BitStreamReader r = new BitStreamReader(buf, version);
        r.setTextCharset(textCharset);

        int objSize = r.readModularShort();
        if (objSize <= 0) return null;

        long handleStreamBits = -1;
        if (version.from(DwgVersion.R2010)) {
            handleStreamBits = r.readUMC();
        }
        long dataStartBit = r.position();

        int typeCode = version.from(DwgVersion.R2010) ? r.readBOT() : r.readBitShort();

        DwgObjectType type = resolveType(typeCode);
        DwgObject obj = createObject(type);
        if (obj == null) return null;

        ((AbstractDwgObject) obj).setHandle(handle);
        ((AbstractDwgObject) obj).setRawTypeCode(typeCode);
        readObjectData(obj, type, r, version, dataStartBit, objSize, handleStreamBits);

        return obj;
    }

    /**
     * 타입 코드를 타입으로 바꾼다. 500 이상은 Classes 섹션의 클래스 번호이므로 그 클래스의 DXF 이름으로 찾는다.
     */
    private DwgObjectType resolveType(int typeCode) {
        if (typeCode >= DwgObjectType.FIRST_CLASS_NUMBER) {
            if (classRegistry == null) {
                return DwgObjectType.UNKNOWN;
            }
            return classRegistry.find(typeCode)
                    .map(def -> DwgObjectType.fromDxfName(def.dxfRecordName()))
                    .orElse(DwgObjectType.UNKNOWN);
        }
        return DwgObjectType.fromCode(typeCode);
    }

    /**
     * 타입 코드 다음 위치에서 객체의 내용을 읽고, 이어서 핸들 스트림을 읽는다.
     * 내용을 읽다가 실패해도 핸들 스트림은 위치가 따로 정해져 있으므로 읽는다.
     *
     * @param dataStartBit     객체 데이터의 시작 비트 위치 (MS 크기와 R2010+의 UMC 다음)
     * @param objSizeBytes     객체 데이터의 바이트 수
     * @param handleStreamBits 핸들 스트림의 비트 수 (R2010+), 그 밖의 버전은 -1
     */
    private void readObjectData(DwgObject obj, DwgObjectType type, BitStreamReader r, DwgVersion version,
                                long dataStartBit, int objSizeBytes, long handleStreamBits) {
        ObjectReader reader = resolver.resolve(type.typeCode()).orElse(null);
        try {
            if (reader == null) {
                // 리더가 없는 비엔티티 객체도 공통 헤더는 읽어 소유자를 알 수 있게 한다
                if (!isEntityClass(obj.rawTypeCode())) {
                    EntityHeaderReader.readObjectHeader(r, version);
                }
            } else {
                if (!reader.readsOwnHeader()) {
                    EntityHeaderReader.readObjectHeader(r, version);
                }
                reader.read(obj, r, version);
            }
        } catch (Exception e) {
            // Type-specific parsing failed silently
        }
        try {
            readHandleStream(obj, reader, r, version, dataStartBit, objSizeBytes, handleStreamBits);
        } catch (Exception e) {
            // Handle stream parsing failed silently
        }
    }

    private boolean isEntityClass(int typeCode) {
        return typeCode >= DwgObjectType.FIRST_CLASS_NUMBER && classRegistry != null
                && classRegistry.find(typeCode).map(def -> def.isEntity()).orElse(false);
    }

    /**
     * 핸들 스트림 : 공통 핸들을 읽고 타입 고유의 핸들은 리더에 맡긴다.
     * 구성은 공통 헤더의 값({@link ObjectHeaderData})에 따라 달라진다.
     * R2000 파일로 검증했다. 그 밖의 버전은 스펙을 따랐지만 검증하지 않았다.
     */
    private void readHandleStream(DwgObject obj, ObjectReader reader, BitStreamReader r, DwgVersion version,
                                  long dataStartBit, int objSizeBytes, long handleStreamBits) throws Exception {
        ObjectHeaderData header = ObjectHeaderData.of(r);
        long endBit = dataStartBit + (long) objSizeBytes * 8L;
        long startBit;
        if (handleStreamBits >= 0) {
            startBit = endBit - handleStreamBits;
        } else if (header.bitSize >= 0) {
            startBit = dataStartBit + header.bitSize;
        } else {
            return;
        }
        if (startBit < dataStartBit || startBit > endBit || header.numReactors < 0 || header.numReactors > 100000) {
            return;
        }

        AbstractDwgObject ao = (AbstractDwgObject) obj;
        long self = obj.handle();
        r.seek(startBit);

        if (header.entity && obj instanceof AbstractDwgEntity) {
            AbstractDwgEntity ae = (AbstractDwgEntity) obj;
            ae.setEntityMode(header.entMode);
            ae.setColor(new CmColor(header.colorIndex));
            ae.setLineTypeScale(header.linetypeScale);
            ae.setInvisibility(header.invisible);
            ae.setLineWeight(header.lineWeight);
            ae.setPlotStyleFlags(header.plotStyleFlags);

            // entmode 0 : 소유자(블록 헤더 또는 상위 엔티티)가 핸들로 기록된다
            if (header.entMode == 0) {
                ao.setOwnerHandle(new DwgHandleRef(r.readHandleReference(self)));
            }
            for (int i = 0; i < header.numReactors; i++) {
                ao.addReactorHandle(new DwgHandleRef(r.readHandleReference(self)));
            }
            if (!version.from(DwgVersion.R2004) || !header.xdicMissing) {
                ao.setXDicHandle(new DwgHandleRef(r.readHandleReference(self)));
            }
            if (!version.from(DwgVersion.R2000)) {
                // R13/R14
                ae.setLayerHandle(new DwgHandleRef(r.readHandleReference(self)));
                if (!header.isByLayerLinetype) {
                    ae.setLineTypeHandle(new DwgHandleRef(r.readHandleReference(self)));
                }
            }
            if (!version.from(DwgVersion.R2004) && !header.noLinks) {
                r.readHandleReference(self);        // previous entity
                r.readHandleReference(self);        // next entity
            }
            if (version.from(DwgVersion.R2010) && (header.colorFlags & 0x40) != 0) {
                r.readHandleReference(self);        // color
            }
            if (version.from(DwgVersion.R2000)) {
                ae.setLayerHandle(new DwgHandleRef(r.readHandleReference(self)));
                if (header.linetypeFlags == 3) {
                    ae.setLineTypeHandle(new DwgHandleRef(r.readHandleReference(self)));
                }
                if (version.from(DwgVersion.R2007)) {
                    if (header.materialFlags == 3) {
                        r.readHandleReference(self);
                    }
                    if (header.shadowFlags == 3) {
                        r.readHandleReference(self);
                    }
                }
                if (header.plotStyleFlags == 3) {
                    r.readHandleReference(self);
                }
            }
            if (version.from(DwgVersion.R2010)) {
                if (header.hasFullVisualStyle) r.readHandleReference(self);
                if (header.hasFaceVisualStyle) r.readHandleReference(self);
                if (header.hasEdgeVisualStyle) r.readHandleReference(self);
            }
        } else if (!header.entity) {
            // 비엔티티 : 소유자(테이블 레코드는 제어 객체), 리액터, 확장 사전
            ao.setOwnerHandle(new DwgHandleRef(r.readHandleReference(self)));
            for (int i = 0; i < header.numReactors; i++) {
                ao.addReactorHandle(new DwgHandleRef(r.readHandleReference(self)));
            }
            if (!version.from(DwgVersion.R2004) || !header.xdicMissing) {
                ao.setXDicHandle(new DwgHandleRef(r.readHandleReference(self)));
            }
        } else {
            return;
        }

        if (reader != null) {
            reader.readHandles(obj, r, version);
        }
    }

    private DwgObject createObject(DwgObjectType type) {
        return switch (type) {
            case TEXT                -> new DwgText();
            case ATTDEF              -> new DwgAttdef();
            case ATTRIB              -> new DwgAttrib();
            case BLOCK               -> new DwgBlock();
            case ENDBLK              -> new DwgBlockEnd();
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
            case UNUSED              -> null;
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
            // 전용 클래스가 없는 타입 : 테이블 제어 객체, 프록시 객체, 알 수 없는 클래스
            case OLEFRAME, BLOCK_CONTROL, LAYER_CONTROL, STYLE_CONTROL, LTYPE_CONTROL, VIEW_CONTROL,
                 UCS_CONTROL, VPORT_CONTROL, APPID_CONTROL, DIMSTYLE_CONTROL, VP_ENT_HDR_CONTROL,
                 VP_ENT_HDR, DUMMY, ACAD_PROXY_OBJECT, UNKNOWN -> new DwgGenericObject(type);
        };
    }

    @Override
    public String sectionName() {
        return SectionType.OBJECTS.sectionName();
    }
}
