package decode;

import structure.header.FileHeader;

/**
 * R2007 및 이후 버전 DWG 파일 디코더
 * 스켈레톤 - 실제 구현은 진행 중
 */
public class DecoderR2007 {
    
    /**
     * R2007 메타데이터 읽기
     */
    public static int readMetaData(byte[] buf, int off, FileHeader hdr) throws DwgParseException {
        // TODO: Implement R2007 metadata reading
        return 0;
    }
    
    /**
     * R2007 파일 헤더 읽기
     */
    public static int readFileHeader(byte[] buf, int off, FileHeader hdr) throws DwgParseException {
        // TODO: Implement R2007 file header reading
        return 0x80;  // R2007 헤더 크기: 128 바이트
    }
}
