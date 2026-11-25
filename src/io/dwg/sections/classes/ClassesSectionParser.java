package io.dwg.sections.classes;

import io.dwg.core.io.BitStreamReader;
import io.dwg.core.io.ByteBufferBitInput;
import io.dwg.core.io.SectionInputStream;
import io.dwg.core.version.DwgVersion;
import io.dwg.format.common.SectionType;
import io.dwg.sections.AbstractSectionParser;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * AcDb:Classes, spec chapter 10 and §5.8.
 * <pre>
 *   sentinel (16 bytes), RL size of the class data in bytes
 *   R2010/R2013 with maintenance version &gt; 3, and R2018+: RL unknown
 *   R2007+: RL size of the data in bits
 *   R2004+: BS maximum class number, RC 0, RC 0, B true
 *   classes, until the data ends:
 *     BS class number, BS version (R14+: proxy flags), TV application name, TV C++ class name,
 *     TV DXF name, B was a zombie, BS item class id (0x1F2 entities, 0x1F3 objects)
 *     R2004+: BL number of objects, BS DWG version, BS maintenance version, BL unknown, BL unknown
 *   R2007+: the strings are in a string stream at the end of the data, located as in objects
 *   RS CRC, end sentinel
 * </pre>
 */
public class ClassesSectionParser extends AbstractSectionParser<List<DwgClassDefinition>> {

    static final byte[] START_SENTINEL = {
        (byte)0x8D, (byte)0xA1, (byte)0xC4, (byte)0xB8, (byte)0xC4, (byte)0xA9,
        (byte)0xF8, (byte)0xC5, (byte)0xC0, (byte)0xDC, (byte)0xF4, (byte)0x5F,
        (byte)0xE7, (byte)0xCF, (byte)0xB6, (byte)0x8A
    };

    private static final int ENTITY_CLASS_ID = 0x1F2;
    private static final int SENTINEL_BITS = 16 * 8;

    private int maintenanceVersion;
    private Charset textCharset;

    /** The maintenance version of the file (file header); it decides one field of R2010/R2013. */
    public void setMaintenanceVersion(int maintenanceVersion) { this.maintenanceVersion = maintenanceVersion; }

    /** The character set of R13–R2004 strings. */
    public void setTextCharset(Charset textCharset) { this.textCharset = textCharset; }

    @Override
    public List<DwgClassDefinition> parse(SectionInputStream stream, DwgVersion version) throws Exception {
        List<DwgClassDefinition> classes = new ArrayList<>();
        byte[] raw = stream.rawBytes();
        ByteBufferBitInput input = new ByteBufferBitInput(raw);
        BitStreamReader r = new BitStreamReader(input, version);
        r.setTextCharset(textCharset);

        validateSentinel(r, START_SENTINEL);
        long size = input.readRawLong() & 0xFFFFFFFFL;
        if (hasUnknownLong(version)) {
            input.readRawLong();
        }
        long dataStart = r.position();
        long dataEnd = dataStart + size * 8;
        if (version.from(DwgVersion.R2007)) {
            long bits = input.readRawLong() & 0xFFFFFFFFL;
            dataEnd = dataStart + bits;
            r.setStringStream(stringStream(raw, dataStart, dataEnd));
            dataEnd = stringStreamStart;
        }
        if (dataEnd > (long) raw.length * 8) {
            issues.accept(io.dwg.api.ReadIssue.section("AcDb:Classes: the data size " + size + " is larger than the section"));
            dataEnd = (long) raw.length * 8;
        }
        if (version.from(DwgVersion.R2004)) {
            r.readBitShort();                      // maximum class number
            r.readRawChar();                       // 0
            r.readRawChar();                       // 0
            input.readBit();                       // true
        }
        while (dataEnd - r.position() >= 8 * 2) {
            classes.add(readClass(r, version));
        }
        return classes;
    }

    private boolean hasUnknownLong(DwgVersion v) {
        return v.from(DwgVersion.R2018) || (v.from(DwgVersion.R2010) && maintenanceVersion > 3);
    }

    private long stringStreamStart;

    /**
     * The R2007+ string stream: the last bit of the data says whether there is one; the 16 bits
     * before it give its size in bits (with bit 15 set, another 16 bits before them hold the
     * high part), and the stream ends where that size field begins.
     */
    private ByteBufferBitInput stringStream(byte[] raw, long dataStart, long dataEnd) {
        stringStreamStart = dataEnd;
        ByteBufferBitInput s = new ByteBufferBitInput(raw);
        long pos = dataEnd - 1;
        s.seek(pos);
        if (!s.readBit()) {
            stringStreamStart = pos;
            return null;
        }
        pos -= 16;
        s.seek(pos);
        long bits = (s.readBits(8) & 0xFF) | ((s.readBits(8) & 0xFF) << 8);
        if ((bits & 0x8000) != 0) {
            pos -= 16;
            s.seek(pos);
            long high = (s.readBits(8) & 0xFF) | ((s.readBits(8) & 0xFF) << 8);
            bits = (bits & 0x7FFF) | (high << 15);
        }
        long start = pos - bits;
        if (start < dataStart) {
            stringStreamStart = pos;
            return null;
        }
        stringStreamStart = start;
        s.seek(start);
        return s;
    }

    private static DwgClassDefinition readClass(BitStreamReader r, DwgVersion v) {
        DwgClassDefinition def = new DwgClassDefinition();
        def.setClassNumber(r.readBitShort());
        def.setVersion(r.readBitShort());
        def.setApplicationName(r.readVariableText());
        def.setCppClassName(r.readVariableText());
        def.setDxfRecordName(r.readVariableText());
        def.setWasAZombie(r.getInput().readBit());
        def.setAnEntity(r.readBitShort() == ENTITY_CLASS_ID);
        if (v.from(DwgVersion.R2004)) {
            def.setInstanceCount(r.readBitLong());
            def.setDwgVersion(r.readBitShort());
            def.setMaintenanceVersion(r.readBitShort());
            r.readBitLong();
            r.readBitLong();
        }
        return def;
    }

    @Override
    public String sectionName() {
        return SectionType.CLASSES.sectionName();
    }
}
