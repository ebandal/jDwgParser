package decode.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import structure.DwgVersion;
import structure.entities.DwgLayout;
import structure.entities.DwgObject;
import structure.entities.DwgObjectType;
import structure.entities.Point2D;

public class LayoutObjectReader implements ObjectReader {

    @Override
    public int objectTypeCode() {
        return DwgObjectType.LAYOUT.typeCode();
    }

    @Override
    public void read(DwgObject target, byte[] data, int offset, DwgVersion version) throws Exception {
        if (!(target instanceof DwgLayout)) return;
        DwgLayout layout = (DwgLayout) target;

        int byteOff = offset;

        // Name (T)
        if (byteOff + 2 <= data.length) {
            String name = readText(data, byteOff);
            layout.setName(name);
            byteOff += 2 + name.length();
        }

        // Tab order (BS)
        if (byteOff + 2 <= data.length) {
            int order = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            layout.setTabOrder(order);
            byteOff += 2;
        }

        // Paper size (2 × BD)
        if (byteOff + 16 <= data.length) {
            double width = readBitDouble(data, byteOff);
            double height = readBitDouble(data, byteOff + 8);
            layout.setPaperSize(new Point2D(width, height));
            byteOff += 16;
        }

        // Margin left (BD)
        if (byteOff + 8 <= data.length) {
            double marginL = readBitDouble(data, byteOff);
            layout.setMarginLeft(marginL);
            byteOff += 8;
        }

        // Margin right (BD)
        if (byteOff + 8 <= data.length) {
            double marginR = readBitDouble(data, byteOff);
            layout.setMarginRight(marginR);
            byteOff += 8;
        }

        // Margin top (BD)
        if (byteOff + 8 <= data.length) {
            double marginT = readBitDouble(data, byteOff);
            layout.setMarginTop(marginT);
            byteOff += 8;
        }

        // Margin bottom (BD)
        if (byteOff + 8 <= data.length) {
            double marginB = readBitDouble(data, byteOff);
            layout.setMarginBottom(marginB);
        }
    }

    private static double readBitDouble(byte[] data, int byteOff) {
        if (byteOff + 8 > data.length) return 0.0;
        long bits = ByteBuffer.wrap(data, byteOff, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
        return Double.longBitsToDouble(bits);
    }

    private static String readText(byte[] data, int byteOff) {
        if (byteOff + 2 > data.length) return "";
        short len = ByteBuffer.wrap(data, byteOff, 2).order(ByteOrder.LITTLE_ENDIAN).getShort();
        if (len <= 0 || byteOff + 2 + len > data.length) return "";
        return new String(data, byteOff + 2, len);
    }
}
