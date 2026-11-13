package io.dwg.sections.objects.readers;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.DwgObject;
import io.dwg.entities.DwgObjectType;
import io.dwg.entities.concrete.DwgHatch;
import io.dwg.sections.objects.EntityHeaderReader;
import io.dwg.sections.objects.ObjectReader;

/**
 * HATCH 엔티티 리더 (타입 0x4C)
 * 닫힌 영역을 패턴이나 색상으로 채우기
 */
public class HatchObjectReader implements ObjectReader {

    @Override
    public int objectType() { return DwgObjectType.HATCH.typeCode(); }

    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        EntityHeaderReader.readEntityHeader(r, v);
        EntityHeaderReader.readCommonEntityData(r, v);

        DwgHatch hatch = (DwgHatch) target;

        // R2004+ : gradient fill data comes first (not verified)
        if (v.from(DwgVersion.R2004)) {
            r.readBitLong();                    // is gradient fill
            r.readBitLong();                    // reserved
            r.readBitDouble();                  // gradient angle
            r.readBitDouble();                  // gradient shift
            r.readBitLong();                    // single color gradient
            r.readBitDouble();                  // gradient tint
            int numColors = count(r.readBitLong());
            for (int i = 0; i < numColors; i++) {
                r.readBitDouble();              // shift value
                r.readBitShort();               // color index
                r.readBitLong();                // rgb
                int colorFlag = r.getInput().readBits(8);
                if ((colorFlag & 1) != 0) r.readVariableText();     // color name
                if ((colorFlag & 2) != 0) r.readVariableText();     // book name
            }
            r.readVariableText();               // gradient name
        }

        hatch.setElevation(r.readBitDouble());
        hatch.setExtrusion(r.read3BitDouble());
        hatch.setPatternName(r.readVariableText());
        hatch.setSolidFill(r.getInput().readBit());
        hatch.setAssociative(r.getInput().readBit());

        int numLoops = count(r.readBitLong());
        hatch.setNumBoundaryPaths(numLoops);
        int boundaryHandles = 0;
        boolean pixelSize = false;
        for (int i = 0; i < numLoops; i++) {
            DwgHatch.Loop loop = new DwgHatch.Loop();
            loop.flags = r.readBitLong();
            pixelSize |= (loop.flags & 4) != 0;
            loop.polyline = (loop.flags & 2) != 0;
            if (loop.polyline) {
                boolean bulges = r.getInput().readBit();
                loop.closed = r.getInput().readBit();
                int n = count(r.readBitLong());
                for (int k = 0; k < n; k++) {
                    double[] p = r.read2RawDouble();
                    loop.vertices.add(new double[] { p[0], p[1], bulges ? r.readBitDouble() : 0.0 });
                }
            } else {
                loop.closed = true;
                int n = count(r.readBitLong());
                for (int k = 0; k < n; k++) {
                    loop.edges.add(readEdge(r, v));
                }
            }
            boundaryHandles += count(r.readBitLong());
            hatch.loops().add(loop);
        }
        hatch.setBoundaryHandleCount(boundaryHandles);

        hatch.setHatchStyle(r.readBitShort());
        hatch.setPatternType(r.readBitShort());
        if (!hatch.isSolidFill()) {
            hatch.setAngle(r.readBitDouble());
            hatch.setScale(r.readBitDouble());
            hatch.setDoubleHatch(r.getInput().readBit());
            int numLines = count(r.readBitShort());
            for (int i = 0; i < numLines; i++) {
                DwgHatch.PatternLine line = new DwgHatch.PatternLine();
                line.angle = r.readBitDouble();
                line.x = r.readBitDouble();
                line.y = r.readBitDouble();
                line.offsetX = r.readBitDouble();
                line.offsetY = r.readBitDouble();
                line.dashes = new double[count(r.readBitShort())];
                for (int k = 0; k < line.dashes.length; k++) {
                    line.dashes[k] = r.readBitDouble();
                }
                hatch.patternLines().add(line);
            }
        }
        if (pixelSize) {
            r.readBitDouble();
        }
        int numSeeds = count(r.readBitLong());
        for (int i = 0; i < numSeeds; i++) {
            r.read2RawDouble();
        }
    }

    @Override
    public void readHandles(DwgObject target, BitStreamReader r, DwgVersion v) throws Exception {
        int n = ((DwgHatch) target).boundaryHandleCount();
        for (int i = 0; i < n; i++) {
            r.readHandleReference(target.handle());
        }
    }

    private static DwgHatch.Edge readEdge(BitStreamReader r, DwgVersion v) throws Exception {
        DwgHatch.Edge e = new DwgHatch.Edge();
        e.type = r.getInput().readBits(8);
        switch (e.type) {
            case DwgHatch.Edge.LINE:
                e.values = new double[] { r.readRawDouble(), r.readRawDouble(), r.readRawDouble(), r.readRawDouble() };
                break;
            case DwgHatch.Edge.ARC:
                e.values = new double[] { r.readRawDouble(), r.readRawDouble(),
                        r.readBitDouble(), r.readBitDouble(), r.readBitDouble() };
                e.counterClockwise = r.getInput().readBit();
                break;
            case DwgHatch.Edge.ELLIPSE:
                e.values = new double[] { r.readRawDouble(), r.readRawDouble(), r.readRawDouble(), r.readRawDouble(),
                        r.readBitDouble(), r.readBitDouble(), r.readBitDouble() };
                e.counterClockwise = r.getInput().readBit();
                break;
            case DwgHatch.Edge.SPLINE: {
                e.degree = r.readBitLong();
                e.rational = r.getInput().readBit();
                e.periodic = r.getInput().readBit();
                int numKnots = count(r.readBitLong());
                int numControl = count(r.readBitLong());
                e.knots = new double[numKnots];
                for (int i = 0; i < numKnots; i++) {
                    e.knots[i] = r.readBitDouble();
                }
                e.controlPoints = new double[numControl][];
                for (int i = 0; i < numControl; i++) {
                    double[] p = r.read2RawDouble();
                    e.controlPoints[i] = new double[] { p[0], p[1], e.rational ? r.readBitDouble() : 1.0 };
                }
                // R2010+ : fit data follows (not verified)
                if (v.from(DwgVersion.R2010)) {
                    int numFit = count(r.readBitLong());
                    for (int i = 0; i < numFit; i++) {
                        r.read2RawDouble();
                    }
                    r.read2RawDouble();         // start tangent
                    r.read2RawDouble();         // end tangent
                }
                break;
            }
            default:
                throw new IllegalStateException("bad HATCH edge type " + e.type);
        }
        return e;
    }

    private static int count(int n) {
        if (n < 0 || n > 10_000_000) {
            throw new IllegalStateException("bad HATCH count " + n);
        }
        return n;
    }
}
