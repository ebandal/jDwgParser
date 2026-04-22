package io.dwg.format.r2000;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * R2000 Objects section extractor.
 * R2000 combines Classes + Handles + Objects into one section.
 * This class separates them using Sentinel-based structure.
 *
 * Spec: §3 (R13-R2000), §10 (Classes), §23 (Handles)
 */
public class R2000SectionExtractor {

    private byte[] objectsSection;
    private int offset;

    public R2000SectionExtractor(byte[] objectsSection) {
        this.objectsSection = objectsSection;
        this.offset = 0;
    }

    /**
     * Extract Classes section from Objects section.
     * Format: [16B sentinel] [4B size LE] [data] [2B CRC LE] [16B sentinel]
     */
    public byte[] extractClasses() throws Exception {
        System.out.printf("[R2000] Extracting Classes section from Objects\n");

        // Classes section starts at offset 0 with Sentinel
        // Format per spec §10 (R13/R14 style Sentinels)
        ByteBuffer buf = ByteBuffer.wrap(objectsSection, offset, objectsSection.length - offset);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Skip start sentinel (16 bytes)
        byte[] startSentinel = new byte[16];
        buf.get(startSentinel);
        System.out.printf("[R2000] Start sentinel at 0x%X\n", offset);

        // Read data size (RL, 4 bytes, little-endian)
        int dataSize = buf.getInt();
        System.out.printf("[R2000] Classes data size: %d bytes (0x%X)\n", dataSize, dataSize);

        // Read class data
        byte[] classesData = new byte[dataSize];
        buf.get(classesData);

        // Read CRC (RS, 2 bytes)
        short crcValue = buf.getShort();
        System.out.printf("[R2000] Classes CRC: 0x%04X\n", crcValue & 0xFFFF);

        // Skip end sentinel (16 bytes)
        byte[] endSentinel = new byte[16];
        buf.get(endSentinel);

        // Update offset for next section
        offset += buf.position();

        System.out.printf("[R2000] Classes section extracted, next offset: 0x%X\n", offset);
        return classesData;
    }

    /**
     * Extract Handles section from Objects section.
     * Returns raw bytes of Handles section (pages with RS_BE size headers).
     */
    public byte[] extractHandles() throws Exception {
        System.out.printf("[R2000] Extracting Handles section from offset 0x%X\n", offset);

        // Handles section is immediately after Classes
        // It consists of pages: [RS_BE size] [data] [RS_BE crc] ...
        // Until: size == 2 (termination)

        ByteBuffer buffer = ByteBuffer.wrap(objectsSection, offset, objectsSection.length - offset);
        buffer.order(ByteOrder.BIG_ENDIAN); // RS_BE uses big-endian

        int handlesStart = offset;
        int pageCount = 0;

        // Read pages until termination (size == 2)
        while (buffer.remaining() >= 2) {
            // Read page size (RS_BE, big-endian)
            short pageSizeBE = buffer.getShort();
            int pageSize = pageSizeBE & 0xFFFF;

            System.out.printf("[R2000] Handles page %d: size=%d (0x%X)\n", pageCount, pageSize, pageSize);

            // If page size is 2, it's the termination marker
            if (pageSize == 2) {
                pageCount++;
                break;
            }

            // Skip page data and CRC
            // pageSize includes the 2-byte size field itself
            int remainingPageBytes = pageSize - 2; // -2 because we already read size
            if (buffer.remaining() < remainingPageBytes) {
                System.out.printf("[R2000] WARNING: Incomplete page at offset 0x%X\n",
                    handlesStart + buffer.position());
                break;
            }

            buffer.position(buffer.position() + remainingPageBytes);
            pageCount++;
        }

        // Extract Handles bytes
        int handlesSize = buffer.position();
        byte[] handlesData = new byte[handlesSize];
        System.arraycopy(objectsSection, handlesStart, handlesData, 0, handlesSize);

        System.out.printf("[R2000] Handles section extracted: %d bytes, %d pages\n", handlesSize, pageCount);

        // Update offset for Objects section
        offset = handlesStart + handlesSize;

        return handlesData;
    }

    /**
     * Extract Objects section (everything remaining after Classes and Handles).
     */
    public byte[] extractObjects() throws Exception {
        System.out.printf("[R2000] Extracting Objects section from offset 0x%X\n", offset);

        int objectsSize = objectsSection.length - offset;
        byte[] objectsData = new byte[objectsSize];
        System.arraycopy(objectsSection, offset, objectsData, 0, objectsSize);

        System.out.printf("[R2000] Objects section extracted: %d bytes\n", objectsSize);

        return objectsData;
    }

    /**
     * Get current extraction offset.
     */
    public int getOffset() {
        return offset;
    }
}
