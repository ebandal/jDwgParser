package decode.reader;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.version.DwgVersion;

import structure.entities.DwgEllipse;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point3D;

public class EllipseObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.ELLIPSE.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, structure.DwgVersion version) throws Exception {
        if (!(target instanceof DwgEllipse)) return;
        DwgEllipse ellipse = (DwgEllipse) target;

        // Create BitStreamReader from byte array
        ByteBufferBitInput bitInput = new ByteBufferBitInput(data);
        DwgVersion ioVersion = mapVersion(version);
        BitStreamReader reader = new BitStreamReader(bitInput, ioVersion);

        // Center (3 × BD per spec §2.5)
        double[] centerData = reader.read3BitDouble();
        ellipse.setCenter(new Point3D(centerData[0], centerData[1], centerData[2]));

        // Major axis vector (3 × BD)
        double[] majorData = reader.read3BitDouble();
        ellipse.setMajorAxisVec(new Point3D(majorData[0], majorData[1], majorData[2]));

        // Extrusion (BE - R2004+ has default handling)
        double[] extrusion = reader.readBitExtrusion();
        ellipse.setExtrusion(extrusion);

        // Axis ratio (BD)
        double axisRatio = reader.readBitDouble();
        ellipse.setAxisRatio(axisRatio);

        // Start parameter (BD)
        double startParam = reader.readBitDouble();
        ellipse.setStartParam(startParam);

        // End parameter (BD)
        double endParam = reader.readBitDouble();
        ellipse.setEndParam(endParam);
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
