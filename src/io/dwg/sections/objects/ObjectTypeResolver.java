package io.dwg.sections.objects;

import io.dwg.sections.classes.DwgClassRegistry;
import io.dwg.sections.objects.readers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 객체 타입 번호 → ObjectReader 인스턴스 매핑.
 */
public class ObjectTypeResolver {
    private final Map<Integer, ObjectReader> readers = new HashMap<>();

    public void register(ObjectReader reader) {
        readers.put(reader.objectType(), reader);
    }

    public void registerCustom(int classNum, ObjectReader reader) {
        readers.put(classNum, reader);
    }

    public Optional<ObjectReader> resolve(int typeCode) {
        return Optional.ofNullable(readers.get(typeCode));
    }

    public static ObjectTypeResolver defaultResolver(DwgClassRegistry classReg) {
        ObjectTypeResolver resolver = new ObjectTypeResolver();
        resolver.register(new TextObjectReader());
        resolver.register(new AttdefObjectReader());
        resolver.register(new AttribObjectReader());
        resolver.register(new SeqEndObjectReader());
        resolver.register(new EndblkObjectReader());
        resolver.register(new InsertObjectReader());
        resolver.register(new MinsertObjectReader());
        resolver.register(new Vertex2DObjectReader());
        resolver.register(new Vertex3DObjectReader());
        resolver.register(new VertexMeshObjectReader());
        resolver.register(new VertexPfaceObjectReader());
        resolver.register(new VertexPfaceFaceObjectReader());
        resolver.register(new Polyline2DObjectReader());
        resolver.register(new Polyline3DObjectReader());
        resolver.register(new ArcObjectReader());
        resolver.register(new CircleObjectReader());
        resolver.register(new LineObjectReader());
        resolver.register(new DimensionOrdinateObjectReader());
        resolver.register(new DimensionLinearObjectReader());
        resolver.register(new DimensionAlignedObjectReader());
        resolver.register(new DimensionAng3ptObjectReader());
        resolver.register(new DimensionAng2lnObjectReader());
        resolver.register(new DimensionRadiusObjectReader());
        resolver.register(new DimensionDiameterObjectReader());
        resolver.register(new PointObjectReader());
        resolver.register(new Face3DObjectReader());
        resolver.register(new PolylinePfaceObjectReader());
        resolver.register(new PolylineMeshObjectReader());
        resolver.register(new SolidObjectReader());
        resolver.register(new TraceObjectReader());
        resolver.register(new ShapeObjectReader());
        resolver.register(new ViewportObjectReader());
        resolver.register(new EllipseObjectReader());
        resolver.register(new SplineObjectReader());
        resolver.register(new RegionObjectReader());
        resolver.register(new Solid3dObjectReader());
        resolver.register(new BodyObjectReader());
        resolver.register(new RayObjectReader());
        resolver.register(new XLineObjectReader());
        resolver.register(new MTextObjectReader());
        resolver.register(new LeaderObjectReader());
        resolver.register(new ToleranceObjectReader());
        resolver.register(new MLineObjectReader());
        resolver.register(new LwPolylineObjectReader());
        resolver.register(new HatchObjectReader());
        resolver.register(new BlockHeaderObjectReader());
        resolver.register(new BlockEndObjectReader());
        resolver.register(new LayerObjectReader());
        resolver.register(new LtypeObjectReader());
        resolver.register(new StyleObjectReader());
        resolver.register(new ViewObjectReader());
        resolver.register(new UcsObjectReader());
        resolver.register(new VportObjectReader());
        resolver.register(new AppIdObjectReader());
        resolver.register(new DimStyleObjectReader());
        resolver.register(new MLineStyleObjectReader());
        resolver.register(new LongTransactionObjectReader());
        resolver.register(new LayoutObjectReader());
        resolver.register(new Ole2frameObjectReader());
        resolver.register(new DictionaryObjectReader());
        resolver.register(new GroupObjectReader());
        resolver.register(new XrecordObjectReader());
        resolver.register(new ImageObjectReader());
        resolver.register(new WipeoutObjectReader());
        resolver.register(new XrefObjectReader());
        resolver.register(new UnderlayObjectReader());
        resolver.register(new SurfaceObjectReader());
        resolver.register(new MeshObjectReader());
        resolver.register(new ScaleObjectReader());
        resolver.register(new VisualStyleObjectReader());
        resolver.register(new FieldObjectReader());
        resolver.register(new ProxyEntityObjectReader());
        resolver.register(new DictionaryVarObjectReader());
        resolver.register(new TableObjectReader());
        resolver.register(new ScaleListObjectReader());
        resolver.register(new TableStyleObjectReader());
        resolver.register(new CellStyleObjectReader());
        resolver.register(new PlotStyleObjectReader());
        resolver.register(new MaterialObjectReader());
        resolver.register(new DataSourceObjectReader());
        resolver.register(new PersSubentManagerObjectReader());
        return resolver;
    }
}
