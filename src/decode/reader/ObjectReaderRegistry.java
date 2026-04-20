package decode.reader;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import structure.DwgVersion;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;

public class ObjectReaderRegistry {
    private static final Logger log = Logger.getLogger(ObjectReaderRegistry.class.getName());
    private static final Map<Integer, ObjectReader> readers = new HashMap<>();

    static {
        register(new LineObjectReader());
        register(new CircleObjectReader());
        register(new ArcObjectReader());
        register(new TextObjectReader());
        register(new InsertObjectReader());
        register(new LayerObjectReader());
        register(new PointObjectReader());
        register(new EllipseObjectReader());
        register(new PolylineObjectReader());
        register(new BlockHeaderObjectReader());
        register(new RayObjectReader());
        register(new LtypeObjectReader());
        register(new LwPolylineObjectReader());
        register(new XLineObjectReader());
        register(new MTextObjectReader());
        register(new Vertex2DObjectReader());
        register(new Polyline3DObjectReader());
        register(new DimensionLinearObjectReader());
        register(new SolidObjectReader());
        register(new TraceObjectReader());
        register(new Vertex3DObjectReader());
        register(new AttribObjectReader());
        register(new MinsertObjectReader());
        register(new ShapeObjectReader());
        register(new Face3DObjectReader());
        register(new StyleObjectReader());
        register(new ViewportObjectReader());
        register(new ViewObjectReader());
        register(new AttdefObjectReader());
        register(new SeqEndObjectReader());
        register(new DimStyleObjectReader());
        register(new AppIdObjectReader());
        register(new VportObjectReader());
        register(new UcsObjectReader());
        register(new BlockEndObjectReader());
        register(new LeaderObjectReader());
        register(new ToleranceObjectReader());
        register(new MLineObjectReader());
        register(new DimensionRadiusObjectReader());
        register(new DimensionDiameterObjectReader());
        register(new DimensionAlignedObjectReader());
        register(new DimensionAng3ptObjectReader());
        register(new VertexMeshObjectReader());
        register(new VertexPfaceObjectReader());
        register(new PolylineMeshObjectReader());
        register(new HatchObjectReader());
        register(new LayoutObjectReader());
        register(new DictionaryObjectReader());
        register(new GroupObjectReader());
        register(new RegionObjectReader());
        register(new BodyObjectReader());
        register(new Solid3dObjectReader());
        register(new SplineObjectReader());
        register(new MLineStyleObjectReader());
    }

    private static void register(ObjectReader reader) {
        readers.put(reader.objectTypeCode(), reader);
        log.fine("Registered ObjectReader for type 0x" + Integer.toHexString(reader.objectTypeCode()));
    }

    public static void readObject(DwgObject obj, byte[] data, int offset, DwgVersion version) {
        if (obj == null || data == null) return;

        int typeCode = -1;

        // Try to get type code from objectType() first
        if (obj.objectType() != null) {
            typeCode = obj.objectType().typeCode();
        }

        // Fallback to rawTypeCode if available
        if (typeCode == -1 && obj instanceof structure.entities.AbstractDwgObject) {
            structure.entities.AbstractDwgObject abstractObj = (structure.entities.AbstractDwgObject) obj;
            typeCode = abstractObj.rawTypeCode();
        }

        // Debug: log all non-Layer objects
        if (!(obj instanceof structure.entities.DwgLayer) && typeCode > 0) {
            log.info("ObjectReader: " + obj.getClass().getSimpleName() + " (type=0x" + Integer.toHexString(typeCode) + "), data=" + data.length + " bytes");
        }

        if (typeCode == -1 || typeCode == 0) {
            log.fine("Cannot determine object type for " + obj.getClass().getSimpleName());
            return;
        }

        ObjectReader reader = readers.get(typeCode);
        if (reader != null) {
            try {
                // Debug: log data for Arc/Circle/Line/Ellipse entities (type codes 0x11, 0x12, 0x13, 0x23)
                if ((typeCode == 0x11 || typeCode == 0x12 || typeCode == 0x13 || typeCode == 0x23) && data.length > 0) {
                    StringBuilder hex = new StringBuilder();
                    for (int i = 0; i < Math.min(48, data.length); i++) {
                        hex.append(String.format("%02x ", data[i] & 0xFF));
                    }
                    log.fine(String.format("Reading %s (type 0x%X) data: %s",
                        obj.getClass().getSimpleName(), typeCode, hex.toString()));
                }

                reader.read(obj, data, offset, version);
                log.fine("Read " + obj.getClass().getSimpleName() + " (type 0x" + Integer.toHexString(typeCode) + ", data=" + data.length + " bytes)");
            } catch (Exception e) {
                log.warning("Failed to read object type 0x" + Integer.toHexString(typeCode) + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            log.fine("No ObjectReader for type 0x" + Integer.toHexString(typeCode) + " (" + obj.getClass().getSimpleName() + ")");
        }
    }
}
