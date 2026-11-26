package io.dwg.sections.header;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point2D;
import io.dwg.core.type.Point3D;
import io.dwg.core.util.Crc16;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;
import io.dwg.sections.objects.readers.Colors;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * The header variables, spec chapter 9: the start sentinel, the size of the data (RL), the data,
 * its CRC (§2.14.1, from the size on, seed 0xC0C1) and the end sentinel. The variables are read
 * in the order of the specification's list ({@link HeaderVariableTable}).
 * <p>R2007+: like an object (§20.1), the data has three streams; the strings are at the end of
 * the data, then the handles. Found on the samples, where the specification differs:
 * <ul>
 *   <li>The size in bits that locates the handle stream ("R2007 Only") is there in every
 *       R2007+ version.</li>
 *   <li>R2010+: the 4 bytes after the size (the specification: R2010/R2013 when the maintenance
 *       version is above 3, and R2018+) are there in all samples; which form a file has is
 *       taken from where the CRC and the end sentinel are.</li>
 * </ul>
 * Each problem (CRC, a field that cannot be read, streams that do not end where they should) is
 * reported as a {@link io.dwg.api.ReadIssue}; the values read so far are kept.
 */
public class HeaderSectionParser extends AbstractSectionParser<HeaderVariables> {

    private static final byte[] START_SENTINEL = {
        (byte) 0xCF, 0x7B, 0x1F, 0x23, (byte) 0xFD, (byte) 0xDE, 0x38, (byte) 0xA9,
        0x5F, 0x7C, 0x68, (byte) 0xB8, 0x4E, 0x6D, 0x33, 0x5F
    };
    private static final byte[] END_SENTINEL = {
        0x30, (byte) 0x84, (byte) 0xE0, (byte) 0xDC, 0x02, 0x21, (byte) 0xC7, 0x56,
        (byte) 0xA0, (byte) 0x83, (byte) 0x97, 0x47, (byte) 0xB1, (byte) 0x92, (byte) 0xCC, (byte) 0xA0
    };
    private static final int CRC_SEED = 0xC0C1;
    private static final int CEPSNTYPE_BY_HANDLE = 3;

    @Override
    public HeaderVariables parse(SectionInputStream stream, DwgVersion version) throws Exception {
        HeaderVariables vars = new HeaderVariables();
        byte[] b = stream.rawBytes();
        if (b.length < 38 || !Arrays.equals(Arrays.copyOf(b, 16), START_SENTINEL)) {
            issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: no start sentinel"));
            return vars;
        }
        ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        long size = bb.getInt(16) & 0xFFFFFFFFL;
        int dataStart = 20;
        if (version.from(DwgVersion.R2010) && !endsAt(b, dataStart + size) && endsAt(b, dataStart + 4 + size)) {
            dataStart += 4;
        }
        long dataEnd = dataStart + size;
        if (!endsAt(b, dataEnd)) {
            issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: no end sentinel after the data"));
            dataEnd = Math.min(dataEnd, b.length);
        } else if ((bb.getShort((int) dataEnd) & 0xFFFF) != Crc16.of(CRC_SEED, b, 16, (int) dataEnd - 16)) {
            issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: CRC does not match"));
        }

        ByteBufferBitInput in = new ByteBufferBitInput(b);
        in.seek(dataStart * 8L);
        in.setLimitBit(dataEnd * 8);
        BitStreamReader r = new BitStreamReader(in, version);
        BitStreamReader handles = r;
        ByteBufferBitInput strings = null;
        long handleStart = -1;
        long stringEnd = -1;
        long dataLimit = dataEnd * 8;
        if (version.from(DwgVersion.R2007)) {
            long bits = in.readRawLong() & 0xFFFFFFFFL;
            vars.set("SIZEINBITS", bits);
            handleStart = dataStart * 8L + bits;
            ByteBufferBitInput h = new ByteBufferBitInput(b);
            h.seek(handleStart);
            h.setLimitBit(dataEnd * 8);
            handles = new BitStreamReader(h, version);
            // the string stream ends before its size and the flag bit, just before the handles
            ByteBufferBitInput s = new ByteBufferBitInput(b);
            s.seek(handleStart - 1);
            dataLimit = handleStart - 1;
            if (s.readBit()) {
                long pos = handleStart - 17;
                s.seek(pos);
                long n = (s.readBits(8) & 0xFF) | ((s.readBits(8) & 0xFF) << 8);
                if ((n & 0x8000) != 0) {
                    pos -= 16;
                    s.seek(pos);
                    long high = (s.readBits(8) & 0xFF) | ((s.readBits(8) & 0xFF) << 8);
                    n = (n & 0x7FFF) | (high << 15);
                }
                s.seek(pos - n);
                s.setLimitBit(pos);
                strings = s;
                stringEnd = pos;
                dataLimit = pos - n;
            }
            r.setStringStream(strings);
        }

        String field = null;
        try {
            for (HeaderVariableTable.Entry e : HeaderVariableTable.ENTRIES) {
                if (!e.presentIn(version) || e.name().equals("SIZEINBITS")) {
                    continue;
                }
                if (e.name().equals("CPSNID")
                        && !Integer.valueOf(CEPSNTYPE_BY_HANDLE).equals(vars.get("CEPSNTYPE"))) {
                    continue;
                }
                field = e.name();
                boolean atEnd = e.type().endsWith("E");
                boolean inHandleStream = e.type().equals("H") || atEnd;
                String type = atEnd ? e.type().substring(0, e.type().length() - 1) : e.type();
                vars.set(e.name(), read(type, inHandleStream ? handles : r));
            }
            field = null;
            checkEnds(version, in.position(), dataLimit, handles.position(), dataEnd, strings, stringEnd);
        } catch (RuntimeException ex) {
            issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: cannot read " + field + ": " + ex.getMessage()));
        }
        fillTypedFields(vars);
        return vars;
    }

    private void checkEnds(DwgVersion v, long dataPos, long dataLimit, long handlePos, long dataEnd,
                           ByteBufferBitInput strings, long stringEnd) {
        if (v.from(DwgVersion.R2007)) {
            if (dataPos != dataLimit) {
                issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: the data stream ends at bit "
                        + dataPos + ", not at " + dataLimit));
            }
            if (strings != null && strings.position() != stringEnd) {
                issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: the string stream has "
                        + (stringEnd - strings.position()) + " bits left"));
            }
            if (dataEnd * 8 - handlePos >= 8) {
                issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: the handle stream has "
                        + (dataEnd * 8 - handlePos) + " bits left"));
            }
        } else if (dataEnd * 8 - dataPos >= 8) {
            issues.accept(io.dwg.api.ReadIssue.section("AcDb:Header: the data has "
                    + (dataEnd * 8 - dataPos) + " bits left"));
        }
    }

    private static boolean endsAt(byte[] b, long end) {
        return end >= 0 && end + 18 <= b.length
                && Arrays.equals(Arrays.copyOfRange(b, (int) end + 2, (int) end + 18), END_SENTINEL);
    }

    private static Object read(String type, BitStreamReader r) {
        switch (type) {
            case "B":   return r.getInput().readBit();
            case "BS":  return r.readBitShort();
            case "BL":  return r.readBitLong();
            case "BLL": return r.readBitLongLong();
            case "BD":  return r.readBitDouble();
            case "RC":  return r.readRawChar();
            case "RS":  return r.readRawShort();
            case "RL":  return r.getInput().readRawLong() & 0xFFFFFFFFL;
            case "TV":  return r.readVariableText();
            case "CMC": return Colors.of(r.readCmColor());
            case "2RD": {
                double[] p = r.read2RawDouble();
                return new Point2D(p[0], p[1]);
            }
            case "3BD": {
                double[] p = r.read3BitDouble();
                return new Point3D(p[0], p[1], p[2]);
            }
            case "H":
            case "HD":  return r.readRef(0);
            default: throw new IllegalStateException("header variable type " + type);
        }
    }

    /** The values that {@link HeaderVariables} also keeps in fields of their own. */
    private static void fillTypedFields(HeaderVariables vars) {
        Object layer = vars.get("CLAYER");
        if (layer instanceof DwgHandleRef) vars.setCurrentLayer((DwgHandleRef) layer);
        Object dimstyle = vars.get("DIMSTYLE");
        if (dimstyle instanceof DwgHandleRef) vars.setDimstyle((DwgHandleRef) dimstyle);
        Object attmode = vars.get("ATTMODE");
        if (attmode instanceof Integer) vars.setAttmode((Integer) attmode != 0);
        Object blipmode = vars.get("BLIPMODE");
        if (blipmode instanceof Boolean) vars.setBlipmode((Boolean) blipmode);
    }

    @Override
    public String sectionName() {
        return SectionType.HEADER.sectionName();
    }
}
