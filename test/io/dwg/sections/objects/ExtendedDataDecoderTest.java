package io.dwg.sections.objects;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.dwg.core.type.DwgHandleRef;
import io.dwg.core.type.Point3D;
import io.dwg.core.version.DwgVersion;
import io.dwg.entities.XDataRecord;

/** The item forms of spec chapter 28, on hand-made data. */
class ExtendedDataDecoderTest {

    @Test
    void readsEveryItemForm() {
        Bytes b = new Bytes();
        b.put(0).put(3).put(30).put(0).ascii("abc");       // string, code page 30
        b.put(2).put(0);                                     // {
        b.put(5).put(0, 0, 0, 0, 0, 0, 0x12, 0x34);          // handle 0x1234
        b.put(4).put(2).put(0xAB, 0xCD);                     // binary
        b.put(10).dbl(1).dbl(2).dbl(3);                      // point
        b.put(40).dbl(0.5);                                  // real
        b.put(70).put(0x34, 0x12);                           // short 0x1234
        b.put(71).put(0x78, 0x56, 0x34, 0x12);               // long 0x12345678
        b.put(2).put(1);                                     // }

        List<XDataRecord> out = new ArrayList<>();
        ExtendedDataDecoder.decode(0x12, b.bytes(), DwgVersion.R2000, StandardCharsets.US_ASCII, out);

        assertEquals(10, out.size());
        assertEquals(1001, out.get(0).groupCode());
        assertEquals(new DwgHandleRef(0x12), out.get(0).value());
        assertEquals("abc", out.get(1).value());
        assertEquals("{", out.get(2).value());
        assertEquals(new DwgHandleRef(0x1234), out.get(3).value());
        assertArrayEquals(new byte[] { (byte) 0xAB, (byte) 0xCD }, (byte[]) out.get(4).value());
        assertEquals(new Point3D(1, 2, 3), out.get(5).value());
        assertEquals(0.5, out.get(6).value());
        assertEquals((short) 0x1234, out.get(7).value());
        assertEquals(0x12345678, out.get(8).value());
        assertEquals("}", out.get(9).value());
    }

    @Test
    void readsUnicodeStringsFromR2007() {
        Bytes b = new Bytes();
        b.put(0).put(2, 0).put('h', 0, 'i', 0);
        b.put(0).put(1, 0).put(0, 0);                        // empty string, stored as one 0
        List<XDataRecord> out = new ArrayList<>();
        ExtendedDataDecoder.decode(1, b.bytes(), DwgVersion.R2007, null, out);
        assertEquals("hi", out.get(1).value());
        assertEquals("", out.get(2).value());
    }

    @Test
    void keepsWhatItCannotRead() {
        Bytes b = new Bytes();
        b.put(70).put(1, 0);
        b.put(99).put(7, 8);                                 // unknown code
        List<XDataRecord> out = new ArrayList<>();
        ExtendedDataDecoder.decode(1, b.bytes(), DwgVersion.R2000, null, out);
        assertEquals(3, out.size());
        assertEquals(ExtendedDataDecoder.UNREAD, out.get(2).groupCode());
        assertArrayEquals(new byte[] { 99, 7, 8 }, (byte[]) out.get(2).value());
    }

    @Test
    void keepsAValueThatRunsPastTheEnd() {
        Bytes b = new Bytes();
        b.put(40).put(1, 2, 3);                              // a real needs 8 bytes
        List<XDataRecord> out = new ArrayList<>();
        ExtendedDataDecoder.decode(1, b.bytes(), DwgVersion.R2000, null, out);
        assertEquals(ExtendedDataDecoder.UNREAD, out.get(1).groupCode());
    }

    private static final class Bytes {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Bytes put(int... values) {
            for (int v : values) out.write(v);
            return this;
        }

        Bytes ascii(String s) {
            out.writeBytes(s.getBytes(StandardCharsets.US_ASCII));
            return this;
        }

        Bytes dbl(double d) {
            long bits = Double.doubleToLongBits(d);
            for (int i = 0; i < 8; i++) out.write((int) (bits >>> (8 * i)));
            return this;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }
}
