package io.dwg.sections.objects;

import io.dwg.core.io.BitStreamReader;

import java.util.ArrayList;
import java.util.List;

/**
 * 객체 하나를 읽는 동안 공통 헤더에서 얻은 값.
 * 핸들 스트림은 이 값들에 따라 구성이 달라지므로, 헤더를 읽을 때 보관해 두었다가 핸들을 읽을 때 쓴다.
 * 객체마다 새 {@link BitStreamReader}에 붙어 있다.
 */
public final class ObjectHeaderData {
    /** 객체 데이터 시작부터 핸들 스트림 시작까지의 비트 수. 알 수 없으면 -1 */
    public long bitSize = -1;

    /** R2007+: 문자열 스트림이 시작하는 비트 위치 (본문 데이터는 여기서 끝난다). 없으면 -1 */
    public long stringStreamStart = -1;
    /** R2007+: 문자열 스트림이 끝나는 비트 위치(크기 필드 앞)와 그 입력. 없으면 -1, null */
    public long stringStreamEnd = -1;
    public io.dwg.core.io.ByteBufferBitInput stringInput;
    /** 타입별 리더가 본문을 다 읽은 위치. 리더가 없거나 실패하면 -1 */
    public long dataEnd = -1;

    /** 객체 데이터의 시작(타입 코드 위치)과 끝(CRC 앞) 비트 위치, R2010+ 핸들 스트림의 비트 수 (없으면 -1) */
    public long dataStart = -1;
    public long objectEnd = -1;
    public long handleStreamBits = -1;

    /** 핸들 스트림이 시작하는 비트 위치. 아직 알 수 없으면 -1 (R2010+는 처음부터, 그 전은 헤더의 RL 뒤에 알 수 있다) */
    public long handleStreamStart() {
        if (handleStreamBits >= 0 && objectEnd >= 0) return objectEnd - handleStreamBits;
        if (bitSize >= 0 && dataStart >= 0) return dataStart + bitSize;
        return -1;
    }

    /**
     * 본문 데이터가 끝나는 비트 위치: R2007+는 문자열 스트림 시작, 문자열이 없으면 핸들 스트림 앞의 한 비트
     * 앞. 그 전 버전은 핸들 스트림 시작. 알 수 없으면 -1
     */
    public long dataLimit(boolean r2007) {
        long handles = handleStreamStart();
        if (handles < 0) return -1;
        if (stringStreamStart >= 0) return stringStreamStart;
        return r2007 ? handles - 1 : handles;
    }

    /**
     * 본문에서 핸들을 가리키는 필드를 만났을 때 리더가 등록하는 자리. 핸들 스트림에서 같은 순서로 채운다
     * ({@code readHandleSlots}).
     */
    public final List<java.util.function.Consumer<io.dwg.core.type.DwgHandleRef>> handleSlots = new ArrayList<>();

    /** EED (스펙 28장): 애플리케이션마다 APPID 핸들과 데이터 바이트 */
    public final List<ExtendedData> extendedData = new ArrayList<>();

    public int numReactors;
    public boolean xdicMissing;

    // 엔티티 전용
    public boolean entity;
    public int entMode;
    public boolean isByLayerLinetype;       // R13/R14
    public boolean noLinks;                 // R13-R2000
    public int colorIndex;
    public int colorFlags;                  // R2004+ : ENC의 상위 바이트
    public double linetypeScale = 1.0;
    public int linetypeFlags;
    public int plotStyleFlags;
    public int materialFlags;
    public int shadowFlags;
    public boolean hasFullVisualStyle;
    public boolean hasFaceVisualStyle;
    public boolean hasEdgeVisualStyle;
    public int invisible;
    public int lineWeight;

    /** 한 애플리케이션의 EED */
    public static final class ExtendedData {
        private final long appHandle;
        private final byte[] data;

        public ExtendedData(long appHandle, byte[] data) {
            this.appHandle = appHandle;
            this.data = data;
        }

        public long appHandle() {
            return appHandle;
        }

        public byte[] data() {
            return data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExtendedData)) return false;
            ExtendedData other = (ExtendedData) o;
            return appHandle == other.appHandle
                    && java.util.Objects.equals(data, other.data);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(appHandle, data);
        }

        @Override
        public String toString() {
            return "ExtendedData[" + "appHandle=" + appHandle + ", " + "data=" + data + "]";
        }
    }

    /** 이 리더로 읽고 있는 객체의 헤더 값. 없으면 만든다. */
    public static ObjectHeaderData of(BitStreamReader r) {
        Object context = r.context();
        if (!(context instanceof ObjectHeaderData)) {
            context = new ObjectHeaderData();
            r.setContext(context);
        }
        return (ObjectHeaderData) context;
    }
}
