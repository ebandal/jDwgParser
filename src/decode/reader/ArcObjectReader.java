package decode.reader;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;

import structure.entities.DwgArc;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class ArcObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.ARC.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, structure.DwgVersion version) throws Exception {
        if (!(target instanceof DwgArc)) return;
        DwgArc arc = (DwgArc) target;

        // Create BitStreamReader from byte array
        ByteBufferBitInput bitInput = new ByteBufferBitInput(data);
        // Map structure.DwgVersion to io.dwg.core.version.DwgVersion
        DwgVersion ioVersion = mapVersion(version);
        BitStreamReader reader = new BitStreamReader(bitInput, ioVersion);

        // Center (3 × BD per spec §2.5)
        double[] centerData = reader.read3BitDouble();
        arc.setCenter(new Point3D(centerData[0], centerData[1], centerData[2]));

        // Radius (BD)
        double radius = reader.readBitDouble();
        arc.setRadius(radius);

        // Thickness (BT - R2004+ has default handling)
        double thickness = reader.readBitThickness();
        arc.setThickness(thickness);

        // Extrusion (BE - R2004+ has default handling)
        double[] extrusion = reader.readBitExtrusion();
        arc.setExtrusion(extrusion);

        // Start angle (BD)
        double startAngle = reader.readBitDouble();
        arc.setStartAngle(startAngle);

        // End angle (BD)
        double endAngle = reader.readBitDouble();
        arc.setEndAngle(endAngle);
    }

    private DwgVersion mapVersion(structure.DwgVersion version) {
        if (version == null) return DwgVersion.R2004;

        switch (version) {
            case R13: return DwgVersion.R13;
            case R14: return DwgVersion.R14;
            case R2000: return DwgVersion.R2000;
            case R2004: return DwgVersion.R2004;
            case R2007: return DwgVersion.R2007;
            case R2010: return DwgVersion.R2010;
            case R2013: return DwgVersion.R2013;
            case R2018: return DwgVersion.R2018;
            default: return DwgVersion.R2004;
        }
    }
}
