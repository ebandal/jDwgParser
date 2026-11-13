package io.dwg.render.r2000;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.dwg.render.r2000.DrawingClass;
import io.dwg.core.util.DwgStringDecoder;
import io.dwg.render.model.DwgObject;
import io.dwg.render.r2000.ObjectType;
import io.dwg.render.r2000.SectionLocator;

/**
 * Walks the object map of an R2000 (AC1015) file and reads the common header
 * (size, type, handle) of every object. It does not decode object bodies.
 *
 * R13/R14 use the same object map but a different object header layout, so
 * they are not accepted here.
 */
public class ObjectScannerR2000 {
    private static final int SECTION_CLASSES = 1;
    private static final int SECTION_OBJECT_MAP = 2;

    public byte[] file;
    /** character set of the texts, from the code page of the drawing */
    public Charset charset = StandardCharsets.ISO_8859_1;
    public List<SectionLocator> sections = new ArrayList<>();
    public Map<Integer, DrawingClass> classes = new LinkedHashMap<>();
    /** handle -> absolute file address, in file order */
    public Map<Long, Long> objectMap = new LinkedHashMap<>();
    public List<DwgObject> objects = new ArrayList<>();

    /** objects whose own handle differs from the handle in the object map */
    public int handleMismatches;
    /** object map entries that could not be read (bad address or truncated data) */
    public int failures;

    public void scan(File dwgFile) throws IOException, DwgParseException {
        file = Files.readAllBytes(dwgFile.toPath());
        if (file.length < 0x19) {
            throw new DwgParseException();
        }
        String versionId = new String(file, 0, 6, StandardCharsets.US_ASCII);
        if (!"AC1015".equals(versionId)) {
            throw new DwgParseException();
        }

        charset = DwgStringDecoder.charsetForDwgCodePage(
                ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN).getShort(0x13));
        readSectionLocators();
        SectionLocator classSection = find(SECTION_CLASSES);
        if (classSection != null) {
            readClasses(classSection);
        }
        SectionLocator mapSection = find(SECTION_OBJECT_MAP);
        if (mapSection == null) {
            throw new DwgParseException();
        }
        readObjectMap(mapSection);
        readObjectHeaders();
    }

    private SectionLocator find(int number) {
        for (SectionLocator sl : sections) {
            if (sl.number == number) {
                return sl;
            }
        }
        return null;
    }

    // 0x15 : number of section locator records, each { RC number, RL seeker, RL size }
    private void readSectionLocators() throws DwgParseException {
        ByteBuffer bb = ByteBuffer.wrap(file).order(ByteOrder.LITTLE_ENDIAN);
        int count = bb.getInt(0x15);
        if (count < 0 || 0x19 + 9L * count > file.length) {
            throw new DwgParseException();
        }
        for (int i = 0; i < count; i++) {
            int p = 0x19 + 9 * i;
            SectionLocator sl = new SectionLocator();
            sl.number = file[p];
            sl.seeker = bb.getInt(p + 1);
            sl.size = bb.getInt(p + 5);
            sections.add(sl);
        }
    }

    // sentinel(16), RL size, class records (bit stream), RS crc, sentinel(16)
    private void readClasses(SectionLocator sl) {
        try {
            BitReader r = new BitReader(file, sl.seeker + 16);
            int size = r.readRL();
            long endBit = (long) (sl.seeker + 16 + 4 + size) * 8;
            // the last record may be followed by a few padding bits
            while (endBit - r.bitPosition() >= 16) {
                DrawingClass dc = new DrawingClass();
                dc.classnum = (short) r.readBS();
                dc.version = (short) r.readBS();
                dc.appname = r.readTV();
                dc.cplusplusclassname = r.readTV();
                dc.classdxfname = r.readTV();
                dc.waszombie = r.readBit();
                dc.itemclassid = (short) r.readBS();
                classes.put((int) dc.classnum, dc);
            }
        } catch (RuntimeException e) {
            // keep the classes read so far; custom types fall back to UNKNOWN_0x..
        }
    }

    // Repeated sections: RS(big-endian) size, { UMC handle delta, MC address delta }*, RS(big-endian) crc.
    // The size counts itself but not the crc; a section of size 2 ends the map.
    // Handle and address accumulate from 0 within each section.
    private void readObjectMap(SectionLocator sl) throws DwgParseException {
        int pos = sl.seeker;
        int end = Math.min(sl.seeker + sl.size, file.length);
        try {
            while (pos + 2 <= end) {
                int sectionSize = ((file[pos] & 0xFF) << 8) | (file[pos + 1] & 0xFF);
                if (sectionSize <= 2) {
                    break;
                }
                int sectionEnd = pos + sectionSize;
                BitReader r = new BitReader(file, pos + 2);
                long handle = 0;
                long address = 0;
                while (r.bytePosition() < sectionEnd) {
                    handle += r.readUMC();
                    address += r.readMC();
                    objectMap.put(handle, address);
                }
                pos = sectionEnd + 2;
            }
        } catch (RuntimeException e) {
            throw new DwgParseException();
        }
    }

    // object : MS size, then bit stream { BS type, RL size in bits, H handle, ... }, RS crc
    private void readObjectHeaders() {
        for (Map.Entry<Long, Long> e : objectMap.entrySet()) {
            long address = e.getValue();
            if (address <= 0 || address + 4 > file.length) {
                failures++;
                continue;
            }
            try {
                BitReader r = new BitReader(file, (int) address);
                DwgObject obj = new DwgObject();
                obj.handle = e.getKey();
                obj.offset = (int) address;
                obj.size = r.readMS();
                obj.dataOffset = r.bytePosition();
                if (obj.size <= 0 || (long) obj.dataOffset + obj.size + 2 > file.length) {
                    failures++;
                    continue;
                }
                obj.type = r.readBS();
                obj.bitSize = r.readRL();
                long ownHandle = r.readHandle()[1];
                obj.bodyBitPos = r.bitPosition();
                obj.typeName = ObjectType.nameOf(obj.type, classes);
                obj.entity = ObjectType.isEntity(obj.type, classes);
                if (ownHandle != obj.handle) {
                    handleMismatches++;
                }
                objects.add(obj);
            } catch (RuntimeException ex) {
                failures++;
            }
        }
    }
}
