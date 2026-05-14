package io.dwg.sections.header;

import io.dwg.core.io.BitStreamWriter;
import io.dwg.core.io.SectionOutputStream;
import io.dwg.core.type.Point2D;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.sections.SectionWriter;

/**
 * Header Section Writer (Spec §9: AcDb:Header)
 * Serializes HeaderVariables to DWG format
 */
public class HeaderSectionWriter implements SectionWriter<HeaderVariables> {

    private static final byte[] START_SENTINEL = {
        (byte)0xCF, (byte)0x7B, (byte)0x1F, (byte)0x23, (byte)0xFD, (byte)0xDE,
        (byte)0x38, (byte)0xA9, (byte)0x5F, (byte)0x7C, (byte)0x68, (byte)0xB8,
        (byte)0x4E, (byte)0x6D, (byte)0x33, (byte)0x5F
    };

    @Override
    public String sectionName() {
        return "AcDb:Header";
    }

    @Override
    public SectionOutputStream write(HeaderVariables vars, DwgVersion version) throws Exception {
        SectionOutputStream section = new SectionOutputStream(sectionName());
        BitStreamWriter writer = section.writer(version);

        // Write sentinel
        for (byte b : START_SENTINEL) {
            writer.getOutput().writeRawChar(b & 0xFF);
        }

        // Write section size (placeholder - should be updated with actual size)
        writer.getOutput().writeRawLong(0);

        // Write common variables
        writeCommonVariables(writer, vars, version);

        // Write version-specific variables
        if (version.from(DwgVersion.R2000)) {
            writeVersionSpecificVariables(writer, vars, version);
        }

        return section;
    }

    private void writeCommonVariables(BitStreamWriter writer, HeaderVariables vars, DwgVersion version) throws Exception {
        writer.writeVariableText(vars.acadVer() != null ? vars.acadVer() : "");

        if (version.from(DwgVersion.R2004)) {
            writer.writeBitLong(0); // ACADMAINTVER
        }

        writer.writeVariableText((String) vars.get("DWGCODEPAGE"));
        writePoint3D(writer, vars.insBase());
        writePoint3D(writer, vars.extMin());
        writePoint3D(writer, vars.extMax());
        writePoint2D(writer, vars.limMin());
        writePoint2D(writer, vars.limMax());
        writer.writeBitShort((Integer) vars.get("ORTHOMODE"));
        writer.writeBitShort((Integer) vars.get("REGENMODE"));
        writer.writeBitShort((Integer) vars.get("FILLMODE"));
        writer.writeBitShort((Integer) vars.get("QTEXTMODE"));
        writer.writeBitShort(vars.attmode() ? 1 : 0);
        writer.writeBitShort((Integer) vars.get("PSLTSCALE"));
        writer.writeBitDouble(vars.ltscale());
        writer.writeBitDouble(vars.dimscale());
        writer.writeBitDouble((Double) vars.get("TEXTSIZE"));
        writer.writeBitDouble((Double) vars.get("TRACEWID"));
        writer.writeVariableText((String) vars.get("TEXTSTYLE"));
        writer.writeBitShort(vars.lunits());
        writer.writeBitShort(vars.luprec());
        writer.writeBitDouble((Double) vars.get("ANGBASE"));
        writer.writeBitShort((Integer) vars.get("ANGDIR"));
        writer.writeBitShort((Integer) vars.get("PDMODE"));
        writer.writeBitDouble((Double) vars.get("PDSIZE"));
        writer.writeBitDouble((Double) vars.get("PLINEWID"));
    }

    private void writeVersionSpecificVariables(BitStreamWriter writer, HeaderVariables vars, DwgVersion version) throws Exception {
        writer.writeBitShort((Integer) vars.get("USERI1"));
        writer.writeBitShort((Integer) vars.get("USERI2"));
        writer.writeBitShort((Integer) vars.get("USERI3"));
        writer.writeBitShort((Integer) vars.get("USERI4"));
        writer.writeBitShort((Integer) vars.get("USERI5"));
        writer.writeBitDouble((Double) vars.get("USERR1"));
        writer.writeBitDouble((Double) vars.get("USERR2"));
        writer.writeBitDouble((Double) vars.get("USERR3"));
        writer.writeBitDouble((Double) vars.get("USERR4"));
        writer.writeBitDouble((Double) vars.get("USERR5"));
        writer.writeBitShort((Integer) vars.get("WORLDVIEW"));
        writer.writeBitShort((Integer) vars.get("SHADEDGE"));
        writer.writeBitShort((Integer) vars.get("SHADEDIF"));
        writer.writeBitShort((Integer) vars.get("TILEMODE"));
        writer.writeBitShort((Integer) vars.get("MAXACTVP"));
        writePoint3D(writer, (Point3D) vars.get("PINSBASE"));
        writer.writeBitShort((Integer) vars.get("PLIMCHECK"));
        writePoint3D(writer, (Point3D) vars.get("PEXTMIN"));
        writePoint3D(writer, (Point3D) vars.get("PEXTMAX"));
        writePoint2D(writer, (Point2D) vars.get("PLIMMIN"));
        writePoint2D(writer, (Point2D) vars.get("PLIMMAX"));
    }

    private void writePoint3D(BitStreamWriter writer, Point3D pt) throws Exception {
        writer.write3BitDouble(new double[]{pt.x(), pt.y(), pt.z()});
    }

    private void writePoint2D(BitStreamWriter writer, Point2D pt) throws Exception {
        writer.write2BitDouble(new double[]{pt.x(), pt.y()});
    }
}
