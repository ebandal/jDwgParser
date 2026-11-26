package io.dwg.sections.objects;

import io.dwg.api.ReadIssue;
import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.XDataRecord;
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

    /**
     * Reads every object the object map (AcDb:Handles) points to. The offsets are byte offsets
     * into the objects data: absolute file addresses for R13–R2000, offsets into AcDb:AcDbObjects
     * for R2004+ (spec §23).
     */
    @Override
    public Map<Long, DwgObject> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<Long, DwgObject> result = new HashMap<>();
        if (handles == null) {
            return result;
        }
        byte[] raw = stream.rawBytes();
        for (io.dwg.sections.handles.HandleEntry entry : handles.sortedEntries()) {
            long offset = entry.offset();
            if (offset < 0 || offset >= raw.length) {
                continue;
            }
            io.dwg.core.io.ReadLimits.checkTime();
            try {
                DwgObject obj = parseObjectAt(raw, (int) offset, version, entry.handle());
                if (obj != null) {
                    result.put(entry.handle(), obj);
                }
            } catch (io.dwg.core.io.ReadLimits.LimitExceededException e) {
                throw e;
            } catch (Exception e) {
                issues.accept(new ReadIssue(ReadIssue.Kind.OBJECT_SKIPPED, entry.handle(), null,
                        "cannot read the object at offset " + offset + ": " + describe(e)));
            }
        }
        nameExtendedDataApplications(result);
        refs.clear();
        return result;
    }

    /**
     * The common handles (owners, layers, line types, ...) point at few objects many times; one
     * DwgHandleRef per handle value is shared while the section is read.
     */
    private final Map<Long, DwgHandleRef> refs = new HashMap<>();

    private DwgHandleRef ref(long handle) {
        return handle == 0 ? DwgHandleRef.NULL : refs.computeIfAbsent(handle, DwgHandleRef::new);
    }

    /**
     * The EED of each application starts with a 1001 record that holds the APPID handle. Once all
     * objects are read, the handle is replaced by the application name, as DXF writes it. A handle
     * whose APPID is not in the file stays as it is.
     */
    private static void nameExtendedDataApplications(Map<Long, DwgObject> objects) {
        Map<Long, String> names = new HashMap<>();
        for (DwgObject obj : objects.values()) {
            if (obj instanceof DwgAppId) {
                names.put(obj.handle(), ((DwgAppId) obj).name());
            }
        }
        for (DwgObject obj : objects.values()) {
            if (!obj.xData().isEmpty()) {
                ((AbstractDwgObject) obj).replaceXData(record -> {
                    if (record.groupCode() == 1001 && record.value() instanceof DwgHandleRef) {
                        String name = names.get(((DwgHandleRef) record.value()).rawHandle());
                        if (name != null) {
                            return new XDataRecord(1001, name);
                        }
                    }
                    return record;
                });
            }
        }
    }

    /**
     * Reads the object at {@code byteOffset} (spec §20.1, §20.2): MS object size in bytes
     * (without the CRC), for R2010+ an unsigned modular char with the size of the handle stream
     * in bits, then the object type (BS; R2010+ the bit-pair form of §2.12) and the data.
     */
    private DwgObject parseObjectAt(byte[] raw, int byteOffset, DwgVersion version, long handle)
            throws Exception {
        ByteBufferBitInput buf = new ByteBufferBitInput(raw);
        buf.seek((long) byteOffset * 8L);
        BitStreamReader r = new BitStreamReader(buf, version);
        r.shareRefs(refs);
        r.setTextCharset(textCharset);

        int objSize = r.readModularShort();
        if (objSize <= 0) return null;

        long handleStreamBits = -1;
        if (version.from(DwgVersion.R2010)) {
            handleStreamBits = r.readUMC();
        }
        long dataStartBit = r.position();
        if (dataStartBit / 8 + objSize > raw.length) {
            throw new IllegalStateException("the object size " + objSize + " runs past the end of the section");
        }

        int typeCode = version.from(DwgVersion.R2010) ? r.readBOT() : r.readBitShort();

        DwgObjectType type = resolveType(typeCode);
        DwgObject obj = createObject(type);
        if (obj == null) return null;

        ((AbstractDwgObject) obj).setHandle(handle);
        ((AbstractDwgObject) obj).setRawTypeCode(typeCode);
        ObjectReader typeReader = resolver.resolve(type.typeCode()).orElse(null);
        if (keepsRawData(type) || (typeReader != null && !typeReader.readsAllData())) {
            int start = (int) (dataStartBit / 8);
            ((AbstractDwgObject) obj).setRawData(java.util.Arrays.copyOfRange(raw, start, Math.min(raw.length, start + objSize)));
        }
        if (obj instanceof DwgGenericObject && keepsRawData(type)) {
            DwgGenericObject generic = (DwgGenericObject) obj;
            if (classRegistry != null) {
                classRegistry.find(typeCode).ifPresent(def -> {
                    generic.setClassName(def.dxfRecordName());
                    generic.setEntity(def.isEntity());
                });
            }
        }
        readObjectData(obj, type, r, version, raw, dataStartBit, objSize, handleStreamBits);

        return obj;
    }

    private static String describe(Exception e) {
        return e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
    }

    /** Objects of these types are kept with their bytes, as nothing interprets their data. */
    private static boolean keepsRawData(DwgObjectType type) {
        return type == DwgObjectType.UNKNOWN || type == DwgObjectType.ACAD_PROXY_OBJECT
                || type == DwgObjectType.ACAD_PROXY_ENTITY;
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
                                byte[] raw, long dataStartBit, int objSizeBytes, long handleStreamBits) {
        ObjectReader reader = resolver.resolve(type.typeCode()).orElse(null);
        // a corrupt count must not make a reader run past the object
        long previousLimit = Long.MAX_VALUE;
        ByteBufferBitInput bounded = r.getInput() instanceof ByteBufferBitInput ? (ByteBufferBitInput) r.getInput() : null;
        if (bounded != null) {
            previousLimit = bounded.limitBit();
            bounded.setLimitBit(dataStartBit + (long) objSizeBytes * 8L);
        }
        try {
            readObjectContent(obj, reader, r, version, raw, dataStartBit, objSizeBytes, handleStreamBits);
        } finally {
            if (bounded != null) {
                bounded.setLimitBit(previousLimit);
            }
        }
    }

    private void readObjectContent(DwgObject obj, ObjectReader reader, BitStreamReader r, DwgVersion version,
                                   byte[] raw, long dataStartBit, int objSizeBytes, long handleStreamBits) {
        ObjectHeaderData position = ObjectHeaderData.of(r);
        position.dataStart = dataStartBit;
        position.objectEnd = dataStartBit + (long) objSizeBytes * 8L;
        position.handleStreamBits = handleStreamBits;
        if (version.from(DwgVersion.R2007)) {
            try {
                attachStringStream(r, version, raw, dataStartBit, objSizeBytes, handleStreamBits);
            } catch (io.dwg.core.io.ReadLimits.LimitExceededException e) {
                throw e;
            } catch (Exception e) {
                r.setStringStream(null);
            }
        }
        try {
            if (reader == null) {
                // 리더가 없는 객체도 공통 헤더는 읽는다: 비엔티티는 소유자를, 엔티티는 EED를 알 수 있다
                if (isEntityClass(obj.rawTypeCode())) {
                    EntityHeaderReader.readEntityHeader(r, version);
                    EntityHeaderReader.readCommonEntityData(r, version);
                } else {
                    EntityHeaderReader.readObjectHeader(r, version);
                }
            } else {
                if (!reader.readsOwnHeader()) {
                    EntityHeaderReader.readObjectHeader(r, version);
                }
                reader.read(obj, r, version);
                if (reader.readsAllData()) {
                    ObjectHeaderData.of(r).dataEnd = r.position();
                }
            }
        } catch (io.dwg.core.io.ReadLimits.LimitExceededException e) {
            throw e;
        } catch (Exception e) {
            issues.accept(new ReadIssue(ReadIssue.Kind.OBJECT_DATA, obj.handle(), obj.objectType().name(), describe(e)));
        }
        addExtendedData(obj, r, version);
        try {
            readHandleStream(obj, reader, r, version, dataStartBit, objSizeBytes, handleStreamBits);
        } catch (io.dwg.core.io.ReadLimits.LimitExceededException e) {
            throw e;
        } catch (Exception e) {
            issues.accept(new ReadIssue(ReadIssue.Kind.OBJECT_HANDLES, obj.handle(), obj.objectType().name(), describe(e)));
        }
    }

    /** 공통 헤더에서 읽어 둔 EED를 객체의 XData로 옮긴다. */
    private void addExtendedData(DwgObject obj, BitStreamReader r, DwgVersion version) {
        ObjectHeaderData header = ObjectHeaderData.of(r);
        if (header.extendedData.isEmpty()) {
            return;
        }
        java.util.List<XDataRecord> records = new java.util.ArrayList<>();
        for (ObjectHeaderData.ExtendedData eed : header.extendedData) {
            ExtendedDataDecoder.decode(eed.appHandle(), eed.data(), version, textCharset, records);
        }
        records.forEach(((AbstractDwgObject) obj)::addXData);
    }

    /**
     * R2007+ : 객체의 문자열은 데이터 끝, 핸들 스트림 바로 앞의 문자열 스트림에 모여 있다.
     * 핸들 스트림 시작 바로 앞 비트가 문자열 유무이고, 그 앞 16비트(크면 32비트)가 스트림의 비트 수이다.
     */
    private void attachStringStream(BitStreamReader r, DwgVersion version, byte[] raw,
                                    long dataStartBit, int objSizeBytes, long handleStreamBits) {
        long endBit = dataStartBit + (long) objSizeBytes * 8L;
        long handleStart;
        if (handleStreamBits >= 0) {
            handleStart = endBit - handleStreamBits;
        } else {
            // R2007 : the size in bits (RL) follows the type code; look at it without consuming it
            long position = r.position();
            long bitSize = 0;
            for (int i = 0; i < 4; i++) {
                bitSize |= (long) (r.getInput().readBits(8) & 0xFF) << (8 * i);
            }
            r.seek(position);
            handleStart = dataStartBit + bitSize;
        }
        if (handleStart <= dataStartBit + 17 || handleStart > endBit) {
            r.setStringStream(null);
            return;
        }

        ByteBufferBitInput s = new ByteBufferBitInput(raw);
        long pos = handleStart - 1;
        s.seek(pos);
        if (!s.readBit()) {
            r.setStringStream(null);
            return;
        }
        pos -= 16;
        s.seek(pos);
        long size = (s.readBits(8) & 0xFF) | ((s.readBits(8) & 0xFF) << 8);
        if ((size & 0x8000) != 0) {
            pos -= 16;
            s.seek(pos);
            long high = (s.readBits(8) & 0xFF) | ((s.readBits(8) & 0xFF) << 8);
            size = (size & 0x7FFF) | (high << 15);
        }
        long start = pos - size;
        if (start < dataStartBit) {
            r.setStringStream(null);
            return;
        }
        s.seek(start);
        s.setLimitBit(pos);                         // the strings end where their size is stored
        ObjectHeaderData.of(r).stringStreamStart = start;
        ObjectHeaderData.of(r).stringStreamEnd = pos;
        ObjectHeaderData.of(r).stringInput = s;
        r.setStringStream(s);
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

        checkDataEnd(obj, header, version, startBit);

        AbstractDwgObject ao = (AbstractDwgObject) obj;
        long self = obj.handle();
        r.seek(startBit);

        if (header.entity && obj instanceof AbstractDwgEntity) {
            AbstractDwgEntity ae = (AbstractDwgEntity) obj;
            ae.setEntityMode(header.entMode);
            ae.setColor(CmColor.fromIndex(header.colorIndex));
            ae.setLineTypeScale(header.linetypeScale);
            ae.setInvisibility(header.invisible);
            ae.setLineWeight(header.lineWeight);
            ae.setPlotStyleFlags(header.plotStyleFlags);

            // entmode 0 : 소유자(블록 헤더 또는 상위 엔티티)가 핸들로 기록된다
            if (header.entMode == 0) {
                ao.setOwnerHandle(ref(r.readHandleReference(self)));
            }
            for (int i = 0; i < header.numReactors; i++) {
                ao.addReactorHandle(ref(r.readHandleReference(self)));
            }
            if (!version.from(DwgVersion.R2004) || !header.xdicMissing) {
                ao.setXDicHandle(ref(r.readHandleReference(self)));
            }
            if (!version.from(DwgVersion.R2000)) {
                // R13/R14
                ae.setLayerHandle(ref(r.readHandleReference(self)));
                if (!header.isByLayerLinetype) {
                    ae.setLineTypeHandle(ref(r.readHandleReference(self)));
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
                ae.setLayerHandle(ref(r.readHandleReference(self)));
                if (header.linetypeFlags == 3) {
                    ae.setLineTypeHandle(ref(r.readHandleReference(self)));
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
            ao.setOwnerHandle(ref(r.readHandleReference(self)));
            for (int i = 0; i < header.numReactors; i++) {
                ao.addReactorHandle(ref(r.readHandleReference(self)));
            }
            if (!version.from(DwgVersion.R2004) || !header.xdicMissing) {
                ao.setXDicHandle(ref(r.readHandleReference(self)));
            }
        } else {
            return;
        }

        if (reader != null) {
            reader.readHandles(obj, r, version);
            if (header.dataEnd >= 0 && endBit - r.position() >= 8) {
                issues.accept(new ReadIssue(ReadIssue.Kind.OBJECT_HANDLES, obj.handle(), obj.objectType().name(),
                        "handles left unread: " + (endBit - r.position()) / 8 + " bytes"));
            }
        }
    }

    /**
     * The data of an object ends where its handle stream begins; in R2007+ the string stream comes
     * before that, and with no string stream one bit (the "has strings" flag) does. A reader that
     * stops elsewhere has read a field wrongly or has left fields out.
     */
    private void checkDataEnd(DwgObject obj, ObjectHeaderData header, DwgVersion version, long handleStart) {
        if (header.dataEnd < 0) {
            return;
        }
        long expected = header.stringStreamStart >= 0 ? header.stringStreamStart
                : version.from(DwgVersion.R2007) ? handleStart - 1 : handleStart;
        long difference = expected - header.dataEnd;
        if (difference != 0) {
            issues.accept(new ReadIssue(ReadIssue.Kind.OBJECT_DATA, obj.handle(), obj.objectType().name(),
                    difference > 0 ? "the data has " + difference + " bits after the fields that were read"
                                   : "the fields that were read run " + (-difference) + " bits past the data"));
        } else if (header.stringInput != null && header.stringInput.position() != header.stringStreamEnd) {
            // the string stream must be read exactly too (reading past it fails on its limit)
            issues.accept(new ReadIssue(ReadIssue.Kind.OBJECT_DATA, obj.handle(), obj.objectType().name(),
                    "the string stream has " + (header.stringStreamEnd - header.stringInput.position())
                    + " bits after the strings that were read"));
        }
    }

    private DwgObject createObject(DwgObjectType type) {
        switch (type) {
            case TEXT:
                return new DwgText();
            case ATTDEF:
                return new DwgAttdef();
            case ATTRIB:
                return new DwgAttrib();
            case BLOCK:
                return new DwgBlock();
            case ENDBLK:
                return new DwgBlockEnd();
            case SEQEND:
                return new DwgSeqEnd();
            case INSERT:
                return new DwgInsert();
            case MINSERT:
                return new DwgMinsert();
            case VERTEX_2D:
                return new DwgVertex2D();
            case VERTEX_3D:
                return new DwgVertex3D();
            case VERTEX_MESH:
                return new DwgVertexMesh();
            case VERTEX_PFACE:
                return new DwgVertexPface();
            case VERTEX_PFACE_FACE:
                return new DwgVertexPfaceFace();
            case POLYLINE_2D:
                return new DwgPolyline2D();
            case POLYLINE_3D:
                return new DwgPolyline3D();
            case ARC:
                return new DwgArc();
            case CIRCLE:
                return new DwgCircle();
            case LINE:
                return new DwgLine();
            case DIMENSION_ORDINATE:
                return new DwgDimensionOrdinate();
            case DIMENSION_LINEAR:
                return new DwgDimensionLinear();
            case DIMENSION_ALIGNED:
                return new DwgDimensionAligned();
            case DIMENSION_ANG_3PT:
                return new DwgDimensionAng3pt();
            case DIMENSION_ANG_2LN:
                return new DwgDimensionAng2ln();
            case DIMENSION_RADIUS:
                return new DwgDimensionRadius();
            case DIMENSION_DIAMETER:
                return new DwgDimensionDiameter();
            case POINT:
                return new DwgPoint();
            case FACE3D:
                return new DwgFace3D();
            case POLYLINE_PFACE:
                return new DwgPolylinePface();
            case POLYLINE_MESH:
                return new DwgPolylineMesh();
            case SOLID:
                return new DwgSolid();
            case TRACE:
                return new DwgTrace();
            case SHAPE:
                return new DwgShape();
            case VIEWPORT:
                return new DwgViewport();
            case ELLIPSE:
                return new DwgEllipse();
            case SPLINE:
                return new DwgSpline();
            case REGION:
                return new DwgRegion();
            case SOLID3D:
                return new DwgSolid3d();
            case BODY:
                return new DwgBody();
            case RAY:
                return new DwgRay();
            case XLINE:
                return new DwgXLine();
            case DICTIONARY:
                return new DwgDictionary();
            case MTEXT:
                return new DwgMText();
            case LEADER:
                return new DwgLeader();
            case TOLERANCE:
                return new DwgTolerance();
            case MLINE:
                return new DwgMLine();
            case BLOCK_HEADER:
                return new DwgBlockHeader();
            case LAYER:
                return new DwgLayer();
            case GROUP:
                return new DwgGroup();
            case OLE2FRAME:
                return new DwgOle2frame();
            case LWPLINE:
                return new DwgLwPolyline();
            case HATCH:
                return new DwgHatch();
            case XRECORD:
                return new DwgXrecord();
            case LTYPE:
                return new DwgLtype();
            case STYLE:
                return new DwgStyle();
            case VIEW:
                return new DwgView();
            case UCS:
                return new DwgUcs();
            case VPORT:
                return new DwgVport();
            case APPID:
                return new DwgAppId();
            case DIMSTYLE:
                return new DwgDimStyle();
            case MLINESTYLE:
                return new DwgMLineStyle();
            case LONG_TRANSACTION:
                return new DwgLongTransaction();
            case LAYOUT:
                return new DwgLayout();
            case PLACEHOLDER:
                return new DwgPlaceholder();
            case VBA_PROJECT:
                return new DwgVbaProject();
            case UNUSED:
                return null;
            case IMAGE:
                return new DwgImage();
            case WIPEOUT:
                return new DwgWipeout();
            case XREF:
                return new DwgXref();
            case UNDERLAY:
                return new DwgUnderlay();
            case SURFACE:
                return new DwgSurface();
            case MESH:
                return new DwgMesh();
            case SCALE:
                return new DwgScale();
            case VISUALSTYLE:
                return new DwgVisualStyle();
            case ACAD_FIELD:
                return new DwgField();
            case ACAD_PROXY_ENTITY:
                return new DwgProxyEntity();
            case ACAD_DICTIONARYVAR:
                return new DwgDictionaryVar();
            case ACAD_TABLE:
                return new DwgTable();
            case ACAD_SCALE_LIST:
                return new DwgScaleList();
            case ACAD_TABLESTYLE:
                return new DwgTableStyle();
            case ACAD_CELLSTYLE:
                return new DwgCellStyle();
            case ACAD_PLOTSTYLE:
                return new DwgPlotStyle();
            case ACAD_MATERIAL:
                return new DwgMaterial();
            case ACAD_DATASOURCE:
                return new DwgDataSource();
            case ACAD_PERSSUBENTMANAGER:
                return new DwgPersSubentManager();
            case MULTILEADER:
                return new DwgMLeader();
            // 전용 클래스가 없는 타입 : 테이블 제어 객체, 프록시 객체, 알 수 없는 클래스
            case OLEFRAME:
            case BLOCK_CONTROL:
            case LAYER_CONTROL:
            case STYLE_CONTROL:
            case LTYPE_CONTROL:
            case VIEW_CONTROL:
            case UCS_CONTROL:
            case VPORT_CONTROL:
            case APPID_CONTROL:
            case DIMSTYLE_CONTROL:
            case VP_ENT_HDR_CONTROL:
            case VP_ENT_HDR:
            case DUMMY:
            case ACAD_PROXY_OBJECT:
            case UNKNOWN:
                return new DwgGenericObject(type);
        }
        throw new IllegalStateException("unexpected " + type);
    }

    @Override
    public String sectionName() {
        return SectionType.OBJECTS.sectionName();
    }
}
