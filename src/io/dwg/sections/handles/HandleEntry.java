package io.dwg.sections.handles;

/**
 * 핸들과 파일 오프셋의 쌍.
 */
public record HandleEntry(long handle, long offset) {}
