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
import io.dwg.sections.AbstractSectionParser;
import io.dwg.sections.classes.DwgClassRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * R2000 Advanced Object Stream Parser
 *
 * Recognizes "AcDb" + classname markers to identify objects
 * Parses each object sequentially without requiring HandleRegistry
 */
public class R2000ObjectStreamAdvancedParser extends AbstractSectionParser<Map<Long, DwgObject>> {

    private ObjectTypeResolver resolver;
    private long nextHandle = 1;  // Handles start at 1

    public R2000ObjectStreamAdvancedParser(DwgClassRegistry classReg) {
        this.resolver = ObjectTypeResolver.defaultResolver(classReg);
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

        System.out.printf("[DEBUG] R2000AdvancedParser: Scanning %d bytes for 'AcDb' markers\n", raw.length);

        int objectCount = 0;

        // Find all "AcDb" markers and parse objects
        for (int offset = 0; offset < raw.length - 4; offset++) {
            // Look for "AcDb" marker
            if (raw[offset] == 'A' && raw[offset + 1] == 'c' && raw[offset + 2] == 'D' && raw[offset + 3] == 'b') {
                try {
                    // Found AcDb marker, read class name
                    String className = readClassNameAt(raw, offset + 4);
                    if (className == null || className.isEmpty()) {
                        continue;
                    }

                    // Try to parse object starting from this offset
                    ParseResult result2 = parseObjectWithClassName(raw, offset, className, version);
                    if (result2 != null && result2.object != null) {
                        result.put(((AbstractDwgObject) result2.object).handle(), result2.object);
                        objectCount++;

                        // Skip ahead to avoid re-parsing the same object
                        offset = result2.nextOffset - 1;

                        if (objectCount % 50 == 0) {
                            System.out.printf("[DEBUG] R2000: Parsed %d objects, offset=0x%X\n", objectCount, offset);
                        }
                    }

                } catch (Exception e) {
                    // Continue searching
                }
            }
        }

        System.out.printf("[DEBUG] R2000AdvancedParser: Parsed %d total objects\n", objectCount);
        return result;
    }

    /**
     * Read null-terminated class name starting after "AcDb"
     */
    private String readClassNameAt(byte[] data, int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < data.length && i < offset + 100; i++) {
            byte b = data[i];
            if (b == 0) {
                // End of string
                return sb.length() > 0 ? sb.toString() : null;
            }
            if (b >= 32 && b < 127) {
                sb.append((char) b);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static class ParseResult {
        DwgObject object;
        int nextOffset;

        ParseResult(DwgObject obj, int next) {
            this.object = obj;
            this.nextOffset = next;
        }
    }

    /**
     * Parse object starting at "AcDb" marker with known class name
     */
    private ParseResult parseObjectWithClassName(byte[] raw, int acdbOffset, String className, DwgVersion version) {
        // The actual object data might start before the "AcDb" marker
        // Try to read object starting from a few bytes before "AcDb"

        int dataStartOffset = Math.max(0, acdbOffset - 50);

        try {
            // Create BitStreamReader
            ByteBufferBitInput buf = new ByteBufferBitInput(
                java.nio.ByteBuffer.wrap(raw, dataStartOffset, Math.min(1000, raw.length - dataStartOffset)));
            BitStreamReader r = new BitStreamReader(buf, version);

            // Skip to the actual data before "AcDb"
            int skipBytes = acdbOffset - dataStartOffset;
            for (int i = 0; i < skipBytes; i++) {
                r.getInput().readRawChar();
            }

            // Now we're at "AcDb"
            // Skip past "AcDb" + classname
            for (int i = 0; i < 4; i++) {
                r.getInput().readRawChar();  // Skip "AcDb"
            }
            while (true) {
                byte b = (byte) r.getInput().readRawChar();
                if (b == 0) break;  // Skip classname
            }

            // Now try to read the object structure
            // Some objects might have size/typeCode, others might not

            // Get type code from class name
            int typeCode = classNameToTypeCode(className);
            if (typeCode < 0) {
                return null;  // Unknown class
            }

            // Create object
            DwgObject obj = createObjectByClassName(className);
            if (obj == null) {
                return null;
            }

            // Set handle and type code
            ((AbstractDwgObject) obj).setHandle(nextHandle++);
            ((AbstractDwgObject) obj).setRawTypeCode(typeCode);

            // Try to parse common header
            try {
                parseCommonHeader(r, obj, version);
            } catch (Exception e) {
                // Common header parsing might fail, but we still have basic object
            }

            // Try type-specific parsing
            resolver.resolve(typeCode).ifPresent(reader -> {
                try {
                    reader.read(obj, r, version);
                } catch (Exception e) {
                    // Type-specific parsing failed
                }
            });

            // Estimate next offset (rough)
            int nextOffset = acdbOffset + 200;

            return new ParseResult(obj, nextOffset);

        } catch (Exception e) {
            return null;
        }
    }

    private int classNameToTypeCode(String className) {
        return switch (className) {
            case "DictionaryWithDefault" -> 0xC3;
            case "TableStyle" -> 0x51;
            case "EvalGraph" -> 0x6F;
            case "AssocDependency" -> 0x9E;
            case "DimAssoc" -> 0xA0;
            case "MLeader" -> 0xDC;
            case "Text" -> 0;
            case "Circle" -> 5;
            case "Line" -> 4;
            case "Layer" -> 0x50;
            case "Style" -> 0x52;
            case "Dictionary" -> 0x0C;
            case "Group" -> 0xA3;
            default -> -1;
        };
    }

    private DwgObject createObjectByClassName(String className) {
        return switch (className) {
            case "Text" -> new DwgText();
            case "Circle" -> new DwgCircle();
            case "Line" -> new DwgLine();
            case "Arc" -> new DwgArc();
            case "Layer" -> new DwgLayer();
            case "Style" -> new DwgStyle();
            case "Dictionary" -> new DwgDictionary();
            case "Group" -> new DwgGroup();
            case "XRecord" -> new DwgXrecord();
            case "MText" -> new DwgMText();
            case "MLeader" -> new DwgLeader();  // Placeholder
            case "Dimension" -> new DwgDimensionLinear();  // Placeholder
            default -> createUnknownObject();
        };
    }

    private DwgObject createUnknownObject() {
        // Return a minimal Dictionary object as placeholder
        return new DwgDictionary();
    }

    private void parseCommonHeader(BitStreamReader r, DwgObject obj, DwgVersion version) throws Exception {
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
                r.getInput().readBits(2);  // lineType flags
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
}
