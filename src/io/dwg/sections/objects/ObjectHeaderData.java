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
    /** 타입별 리더가 본문을 다 읽은 위치. 리더가 없거나 실패하면 -1 */
    public long dataEnd = -1;

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
    public record ExtendedData(long appHandle, byte[] data) {
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
