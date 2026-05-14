package io.dwg.sections.header;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;

/**
 * 스펙 §9 AcDb:Header — HEADER VARIABLES 파서.
 */
public class HeaderSectionParser extends AbstractSectionParser<HeaderVariables> {

    private static final byte[] START_SENTINEL = {
        (byte)0xCF, (byte)0x7B, (byte)0x1F, (byte)0x23, (byte)0xFD, (byte)0xDE,
        (byte)0x38, (byte)0xA9, (byte)0x5F, (byte)0x7C, (byte)0x68, (byte)0xB8,
        (byte)0x4E, (byte)0x6D, (byte)0x33, (byte)0x5F
    };

    @Override
    public HeaderVariables parse(SectionInputStream stream, DwgVersion version) throws Exception {
        HeaderVariables vars = new HeaderVariables();
        BitStreamReader r = reader(stream, version);

        // Sentinel
        validateSentinel(r, START_SENTINEL);

        // 섹션 데이터 크기 (RL) – 향후 경계 검증에 사용
        r.getInput().readRawLong();

        readCommonVariables(r, vars, version);
        if (version.from(DwgVersion.R2000)) {
            readVersionSpecificVariables(r, vars, version);
        }

        return vars;
    }

    private void readCommonVariables(BitStreamReader r, HeaderVariables vars, DwgVersion v) {
        try {
            vars.setAcadVer(r.readVariableText());
            if (v.from(DwgVersion.R2004)) {
                r.readBitLong(); // ACADMAINTVER
            }
            vars.set("DWGCODEPAGE", r.readVariableText());
            vars.setInsBase(readPoint3D(r));
            vars.setExtMin(readPoint3D(r));
            vars.setExtMax(readPoint3D(r));
            vars.setLimMin(readPoint2D(r));
            vars.setLimMax(readPoint2D(r));
            vars.set("ORTHOMODE",  r.readBitShort());
            vars.set("REGENMODE",  r.readBitShort());
            vars.set("FILLMODE",   r.readBitShort());
            vars.set("QTEXTMODE",  r.readBitShort());
            vars.setAttmode(r.readBitShort() != 0);
            vars.set("PSLTSCALE",  r.readBitShort());
            vars.setLtscale(r.readBitDouble());
            vars.setDimscale(r.readBitDouble());
            vars.set("TEXTSIZE",   r.readBitDouble());
            vars.set("TRACEWID",   r.readBitDouble());
            vars.set("TEXTSTYLE",  r.readVariableText());
            vars.setLunits(r.readBitShort());
            vars.setLuprec(r.readBitShort());
            vars.set("ANGBASE",    r.readBitDouble());
            vars.set("ANGDIR",     r.readBitShort());
            vars.set("PDMODE",     r.readBitShort());
            vars.set("PDSIZE",     r.readBitDouble());
            vars.set("PLINEWID",   r.readBitDouble());
        } catch (Exception e) {
            // 버전 차이로 인한 파싱 오류는 현재까지 읽은 값 유지
        }
    }

    private void readVersionSpecificVariables(BitStreamReader r, HeaderVariables vars, DwgVersion v) {
        try {
            // R2000+ 추가 변수들
            vars.set("USERI1", r.readBitShort());
            vars.set("USERI2", r.readBitShort());
            vars.set("USERI3", r.readBitShort());
            vars.set("USERI4", r.readBitShort());
            vars.set("USERI5", r.readBitShort());
            vars.set("USERR1", r.readBitDouble());
            vars.set("USERR2", r.readBitDouble());
            vars.set("USERR3", r.readBitDouble());
            vars.set("USERR4", r.readBitDouble());
            vars.set("USERR5", r.readBitDouble());
            vars.set("WORLDVIEW", r.readBitShort());
            vars.set("SHADEDGE",  r.readBitShort());
            vars.set("SHADEDIF",  r.readBitShort());
            vars.set("TILEMODE",  r.readBitShort());
            vars.set("MAXACTVP",  r.readBitShort());
            vars.set("PINSBASE",  readPoint3D(r));
            vars.set("PLIMCHECK", r.readBitShort());
            vars.set("PEXTMIN",   readPoint3D(r));
            vars.set("PEXTMAX",   readPoint3D(r));
            vars.set("PLIMMIN",   readPoint2D(r));
            vars.set("PLIMMAX",   readPoint2D(r));
            vars.set("UNITMODE",  r.readBitShort());
            vars.set("VISRETAIN", r.readBitShort());

            // Handle references
            vars.setCurrentLayer(new DwgHandleRef(r.readHandle()));
            vars.setCurrentLineType(r.readVariableText());
            vars.setDimstyle(new DwgHandleRef(r.readHandle()));
        } catch (Exception e) {
            // 파싱 오류 무시
        }
    }

    private Point3D readPoint3D(BitStreamReader r) {
        double[] v = r.read3BitDouble();
        return new Point3D(v[0], v[1], v[2]);
    }

    private Point2D readPoint2D(BitStreamReader r) {
        double[] v = r.read2BitDouble();
        return new Point2D(v[0], v[1]);
    }

    @Override
    public String sectionName() {
        return SectionType.HEADER.sectionName();
    }
}
