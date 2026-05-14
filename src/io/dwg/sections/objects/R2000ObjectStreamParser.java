package io.dwg.sections.objects;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.AbstractDwgEntity;
import io.dwg.entities.AbstractDwgObject;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.*;
import io.dwg.sections.AbstractSectionParser;
import io.dwg.sections.classes.DwgClassRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * R2000 Object Stream Parser - R2000 format stores objects in a continuous stream
 * without separate Classes/Handles sections like R13.
 *
 * Parses objects sequentially from the stream without requiring HandleRegistry.
 */
public class R2000ObjectStreamParser extends AbstractSectionParser<Map<Long, DwgObject>> {

    private ObjectTypeResolver resolver;
    private long nextHandle = 0;  // For auto-assigning handles during parsing

    public R2000ObjectStreamParser() {
        this(new DwgClassRegistry());
    }

    public R2000ObjectStreamParser(DwgClassRegistry classReg) {
        this.resolver = ObjectTypeResolver.defaultResolver(classReg);
    }

    public void setClassRegistry(DwgClassRegistry classRegistry) {
        this.resolver = ObjectTypeResolver.defaultResolver(classRegistry);
    }

    @Override
    public String sectionName() {
        return "AcDb:AcDbObjects";
    }

    @Override
    public boolean supports(DwgVersion version) {
        return version == DwgVersion.R2000;
    }

    @Override
    public Map<Long, DwgObject> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        Map<Long, DwgObject> result = new HashMap<>();

        byte[] raw = stream.rawBytes();
        int offset = 0;

        System.out.printf("[DEBUG] R2000ObjectStreamParser: Parsing %d bytes\n", raw.length);

        int objectCount = 0;
        while (offset < raw.length - 4) {
            try {
                // Try to parse object at current offset
                ParseResult parseResult = parseObjectAt(raw, offset, version);

                if (parseResult == null || parseResult.object == null) {
                    // If we can't parse, skip a byte and try again
                    offset++;
                    continue;
                }

                // Successfully parsed an object
                long handle = ((AbstractDwgObject) parseResult.object).handle();
                result.put(handle, parseResult.object);

                offset = parseResult.nextOffset;
                objectCount++;

                if (objectCount % 100 == 0) {
                    System.out.printf("[DEBUG] R2000: Parsed %d objects, offset=0x%X\n", objectCount, offset);
                }

            } catch (Exception e) {
                // Skip problematic areas and continue
                offset++;
            }
        }

        System.out.printf("[DEBUG] R2000ObjectStreamParser: Parsed %d total objects\n", objectCount);
        return result;
    }

    private static class ParseResult {
        DwgObject object;
        int nextOffset;

        ParseResult(DwgObject obj, int next) {
            this.object = obj;
            this.nextOffset = next;
        }
    }

    private ParseResult parseObjectAt(byte[] raw, int byteOffset, DwgVersion version) throws Exception {
        if (byteOffset >= raw.length - 4) {
            return null;  // Not enough data
        }

        try {
            // Create BitStreamReader for this section
            io.dwg.core.io.ByteBufferBitInput buf = new io.dwg.core.io.ByteBufferBitInput(
                java.nio.ByteBuffer.wrap(raw, byteOffset, raw.length - byteOffset));
            BitStreamReader r = new BitStreamReader(buf, version);

            // Try to read object size (MS - modular short)
            int objSize = r.readModularShort();
            if (objSize <= 0 || objSize > 0x100000) {  // Sanity check: object shouldn't be > 1MB
                return null;
            }

            // Read type code (BS - bit short)
            int typeCode = r.readBitShort();

            // Check if this is a valid type code (0-500 is typical range)
            if (typeCode < 0 || typeCode > 999) {
                return null;
            }

            // Create object
            DwgObject obj = createObject(typeCode);
            if (obj == null) {
                return null;
            }

            // Set handle (auto-assigned sequential)
            ((AbstractDwgObject) obj).setHandle(nextHandle++);
            ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

            // Parse common header
            parseCommonHeader(r, obj, version);

            // Parse type-specific data
            resolver.resolve(typeCode).ifPresent(reader -> {
                try {
                    reader.read(obj, r, version);
                } catch (Exception e) {
                    // Type-specific parsing failed, but we have basic object data
                }
            });

            // Calculate next offset: objSize is the data size (doesn't include the 2-byte size field)
            int nextOffset = byteOffset + 2 + objSize;

            // Sanity check
            if (nextOffset > raw.length) {
                return null;
            }

            return new ParseResult(obj, nextOffset);

        } catch (Exception e) {
            return null;
        }
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

        // Entity common header
        if (obj.isEntity() && obj instanceof AbstractDwgEntity) {
            AbstractDwgEntity ae = (AbstractDwgEntity) obj;
            int entityMode = r.getInput().readBits(2);
            ae.setEntityMode(entityMode);

            if (version.from(DwgVersion.R2000)) {
                r.getInput().readBits(2);  // lineType flags (not stored)
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
}
