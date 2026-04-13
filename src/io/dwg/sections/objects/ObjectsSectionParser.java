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

        if (handles == null) return result;

        byte[] raw = stream.rawBytes();
        for (Map.Entry<Long, Long> entry : sortedHandleOffsets()) {
            long handle = entry.getKey();
            long offset = entry.getValue();

            if (offset >= raw.length) continue;

            try {
                DwgObject obj = parseObjectAt(raw, (int) offset, version, handle);
                if (obj != null) {
                    result.put(handle, obj);
                }
            } catch (Exception e) {
                // 파싱 실패 시 해당 객체 건너뜀
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
            case LINE -> new DwgLine();
            case CIRCLE -> new DwgCircle();
            case ARC -> new DwgArc();
            case TEXT -> new DwgText();
            case INSERT -> new DwgInsert();
            case LAYER -> new DwgLayer();
            default -> null;
        };
    }

    @Override
    public String sectionName() {
        return SectionType.OBJECTS.sectionName();
    }
}
