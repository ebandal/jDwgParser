# dwg-core — 클래스 명세

> 비트스트림 I/O, 공통 타입, 버전 감지, 유틸리티, 예외 정의.  
> 스펙 §2 (BIT CODES AND DATA DEFINITIONS) 전체를 구현한다.

---

## 패키지: `io.dwg.core.io`

---

### `BitInput` *(interface)*

비트 단위 읽기 계약을 정의하는 최상위 인터페이스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean readBit()` | 1비트 읽기. 내부 bitOffset 1 증가 |
| `int readBits(int n)` | n비트를 읽어 int로 반환 (MSB first). n ≤ 32 |
| `int readRawChar()` | 비압축 1바이트(RC) 읽기 |
| `short readRawShort()` | 비압축 2바이트(RS) little-endian 읽기 |
| `int readRawLong()` | 비압축 4바이트(RL) little-endian 읽기 |
| `double readRawDouble()` | 비압축 8바이트(RD) IEEE 754 읽기 |
| `long position()` | 현재 비트 위치 반환 |
| `void seek(long bitPos)` | 지정 비트 위치로 이동 |
| `boolean isEof()` | 스트림 끝 여부 |

---

### `BitOutput` *(interface)*

비트 단위 쓰기 계약을 정의하는 최상위 인터페이스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void writeBit(boolean bit)` | 1비트 쓰기. 내부 bitOffset 1 증가 |
| `void writeBits(int value, int n)` | value의 하위 n비트 쓰기 (MSB first) |
| `void writeRawChar(int v)` | 비압축 1바이트 쓰기 |
| `void writeRawShort(short v)` | 비압축 2바이트 little-endian 쓰기 |
| `void writeRawLong(int v)` | 비압축 4바이트 little-endian 쓰기 |
| `void writeRawDouble(double v)` | 비압축 8바이트 IEEE 754 쓰기 |
| `byte[] toByteArray()` | 쓴 내용을 바이트 배열로 반환 |
| `long position()` | 현재 비트 위치 반환 |

---

### `ByteBufferBitInput`

`BitInput` 구현체. `java.nio.ByteBuffer`를 내부 버퍼로 사용.

**필드**
- `ByteBuffer buffer` — 읽기 소스
- `long bitOffset` — 현재 비트 위치 (0-based, 전체 스트림 기준)
- `int currentByte` — 현재 처리 중인 바이트 캐시
- `int bitsRemainingInByte` — 현재 바이트에서 남은 비트 수

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `ByteBufferBitInput(ByteBuffer buf)` | 버퍼 wrap, bitOffset=0 초기화 |
| `ByteBufferBitInput(byte[] bytes)` | `ByteBuffer.wrap(bytes)` 후 위임 |
| `boolean readBit()` | bitsRemainingInByte==0 이면 nextByte() 호출 후 MSB 추출 |
| `int readBits(int n)` | readBit() 루프 또는 최적화된 바이트 단위 읽기 |
| `void seek(long bitPos)` | `buffer.position((int)(bitPos/8))`, bitsRemainingInByte 재계산 |
| `ByteBufferBitInput slice(long startBit, long lengthBits)` | 지정 범위의 서브 스트림 반환 (섹션 분리에 사용) |
| `private void loadNextByte()` | buffer에서 1바이트 로드, currentByte/bitsRemainingInByte 갱신 |

---

### `ByteBufferBitOutput`

`BitOutput` 구현체. 내부적으로 `ByteArrayOutputStream` + 비트 누적 버퍼 사용.

**필드**
- `ByteArrayOutputStream baos`
- `int pendingByte` — 아직 완성되지 않은 바이트
- `int bitsUsedInPending` — pendingByte에 채워진 비트 수

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `ByteBufferBitOutput()` | baos 초기화, pendingByte=0, bitsUsed=0 |
| `void writeBit(boolean bit)` | pendingByte에 비트 추가. bitsUsed==8 이면 flush |
| `void writeBits(int value, int n)` | 상위 비트부터 n번 writeBit 호출 |
| `void flush()` | pendingByte가 있으면 0-패딩 후 baos에 write |
| `byte[] toByteArray()` | flush() 후 baos.toByteArray() 반환 |

---

### `BitStreamReader`

DWG 스펙 §2의 모든 압축 타입 읽기를 제공하는 핵심 클래스.  
내부적으로 `BitInput`을 사용하며, 모든 타입 디코딩 로직을 집중한다.

**필드**
- `BitInput input` — 위임할 비트 입력 소스
- `DwgVersion version` — 버전별 분기에 사용

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `BitStreamReader(BitInput input, DwgVersion version)` | 필드 초기화 |
| `int readBitShort()` | §2.2: 2비트 opcode 읽기 → 00=short(2B), 01=uchar(1B), 10=0, 11=256 |
| `int readBitLong()` | §2.3: 2비트 opcode → 00=long(4B), 01=uchar(1B), 10=0 |
| `long readBitLongLong()` | §2.4: 1~3비트 길이 opcode → l바이트 little-endian 읽기 |
| `double readBitDouble()` | §2.5: 2비트 opcode → 00=double(8B), 01=1.0, 10=0.0 |
| `double readBitDoubleWithDefault(double def)` | §2.9: 2비트 opcode, default 기반 패치 처리 |
| `int readModularChar()` | §2.6: high bit=계속 플래그, 0x40=음수 플래그, little-endian 조합 |
| `int readModularShort()` | §2.7: modular char와 동일 원리, 기본 단위=short(2B) |
| `double[] readBitExtrusion()` | §2.8: R13-14=3BD, R2000+=single bit → 0,0,1 or 3BD |
| `double readBitThickness()` | §2.10: R13-14=BD, R2000+=bit → 0.0 or BD |
| `int[] readCmColor()` | §2.11: R15이하=BS, R2004+=BS+BL+RC 구조 |
| `long readHandle()` | handle reference 읽기: 코드(4bit)+counter(4bit)+값 |
| `long readHandleRef(long ownerHandle)` | offset 방식 handle 참조 해석 |
| `String readText()` | T: BS길이 + 해당 길이만큼 RC 읽기 |
| `String readUnicodeText()` | TU: BS 문자수 + UTF-16LE 읽기 (R2007+) |
| `String readVariableText()` | TV: version < R2007 → readText(), 이상 → readUnicodeText() |
| `double[] read2BitDouble()` | 2BD: readBitDouble() × 2 |
| `double[] read3BitDouble()` | 3BD: readBitDouble() × 3 |
| `double[] read2RawDouble()` | 2RD: readRawDouble() × 2 |
| `double[] read3RawDouble()` | 3RD: readRawDouble() × 3 |
| `long position()` | input.position() 위임 |
| `void seek(long bitPos)` | input.seek(bitPos) 위임 |

---

### `BitStreamWriter`

DWG 스펙 §2의 모든 압축 타입 쓰기를 제공하는 핵심 클래스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `BitStreamWriter(BitOutput output, DwgVersion version)` | 필드 초기화 |
| `void writeBitShort(int value)` | value에 따라 opcode 선택 후 인코딩 |
| `void writeBitLong(int value)` | value에 따라 opcode 선택 후 인코딩 |
| `void writeBitLongLong(long value)` | 필요한 바이트 수 계산 후 opcode + 데이터 쓰기 |
| `void writeBitDouble(double value)` | 0.0/1.0 특수 처리, 나머지 full double |
| `void writeBitDoubleWithDefault(double value, double def)` | def와 비교해 최소 바이트로 패치 인코딩 |
| `void writeModularChar(int value)` | 음수 처리(0x40 플래그) + high-bit 연속 인코딩 |
| `void writeModularShort(int value)` | short 단위 modular 인코딩 |
| `void writeBitExtrusion(double[] vec)` | [0,0,1] 이면 1비트, 아니면 0비트+3BD |
| `void writeBitThickness(double value)` | 0.0 이면 1비트, 아니면 0비트+BD |
| `void writeCmColor(int[] color)` | 버전에 따라 BS 또는 BS+BL+RC 쓰기 |
| `void writeHandle(long handle)` | handle 인코딩 후 쓰기 |
| `void writeText(String text)` | BS길이 + RC 배열 쓰기 |
| `void writeUnicodeText(String text)` | BS 문자수 + UTF-16LE 쓰기 |
| `void writeVariableText(String text)` | 버전에 따라 writeText/writeUnicodeText 분기 |
| `byte[] toByteArray()` | output.toByteArray() 위임 |

---

### `SectionInputStream`

특정 DWG 섹션 데이터를 감싸는 스트림. 섹션 범위 밖 읽기 방지 및 오프셋 추적.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SectionInputStream(byte[] data, String sectionName)` | ByteBufferBitInput 생성, 섹션명 저장 |
| `BitStreamReader reader(DwgVersion ver)` | 이 섹션용 BitStreamReader 생성 |
| `int size()` | 섹션 바이트 크기 반환 |
| `String sectionName()` | 섹션 이름 반환 |
| `byte[] rawBytes()` | 원본 바이트 배열 반환 |

---

### `SectionOutputStream`

섹션 쓰기용 스트림. 완성 후 바이트 배열 추출.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SectionOutputStream(String sectionName)` | ByteBufferBitOutput 생성 |
| `BitStreamWriter writer(DwgVersion ver)` | 이 섹션용 BitStreamWriter 생성 |
| `byte[] toByteArray()` | 완성된 섹션 바이트 반환 |

---

## 패키지: `io.dwg.core.type`

---

### `DwgHandle`

DWG 객체 핸들. 파일 내 모든 객체의 고유 식별자.

**필드**
- `long value` — 핸들 값 (최대 8바이트)
- `int code` — 참조 코드 (0=absolute, 1=owner, 2=owned, 4=prev, 8=next 등)

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgHandle(int code, long value)` | 필드 초기화 |
| `static DwgHandle absolute(long value)` | code=0인 절대 핸들 팩토리 |
| `long value()` | 핸들 값 반환 |
| `int code()` | 참조 코드 반환 |
| `boolean isAbsolute()` | code == 0 여부 |
| `long resolve(long ownerHandle)` | code 기반으로 실제 절대 핸들 계산 (offset 방식) |
| `String toString()` | "Handle[code=X, value=0xYYYY]" 형식 |

---

### `DwgHandleRef`

Handle 참조를 나타내는 값 객체. 지연 해석(lazy resolve)을 위해 실제 객체 대신 핸들만 보관.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgHandleRef(long rawHandle)` | 원시 핸들 값 저장 |
| `long rawHandle()` | 저장된 핸들 값 반환 |
| `boolean isNull()` | rawHandle == 0 여부 |

---

### `Point2D` *(record)*

2D 좌표 (2BD, 2RD 에 대응).

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Point2D(double x, double y)` | record 생성자 |
| `Point2D translate(double dx, double dy)` | 이동 후 새 Point2D 반환 |
| `double distanceTo(Point2D other)` | 두 점 거리 계산 |
| `static Point2D ORIGIN` | (0.0, 0.0) 상수 |

---

### `Point3D` *(record)*

3D 좌표 (3BD, 3RD 에 대응).

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Point3D(double x, double y, double z)` | record 생성자 |
| `Point3D translate(double dx, double dy, double dz)` | 이동 후 새 Point3D 반환 |
| `Point2D toPoint2D()` | z 버리고 Point2D 반환 |
| `static Point3D ORIGIN` | (0.0, 0.0, 0.0) 상수 |

---

### `CmColor`

DWG 색상 표현. §2.11 CMC 구조 대응.

**필드**
- `int colorIndex` — ACI 색상 인덱스 (0~256)
- `int rgb` — R2004+ RGB 값 (0xRRGGBB)
- `byte colorType` — 색상 타입 바이트
- `String colorName` — 색상 이름 (옵션)
- `String bookName` — 색상 책 이름 (옵션)

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static CmColor fromIndex(int index)` | ACI 인덱스 기반 생성 |
| `static CmColor fromRgb(int rgb)` | RGB 기반 생성 (R2004+) |
| `boolean isByLayer()` | colorIndex == 256 여부 |
| `boolean isByBlock()` | colorIndex == 0 여부 |
| `int toArgb()` | ARGB 정수로 변환 |

---

## 패키지: `io.dwg.core.version`

---

### `DwgVersion` *(enum)*

지원하는 모든 DWG 버전 열거. 파일 헤더 버전 문자열과 매핑.

| 상수 | 버전 문자열 | 설명 |
|---|---|---|
| `R13` | `AC1012` | AutoCAD R13 |
| `R14` | `AC1014` | AutoCAD R14 |
| `R2000` | `AC1015` | AutoCAD 2000 |
| `R2004` | `AC1018` | AutoCAD 2004 |
| `R2007` | `AC1021` | AutoCAD 2007 |
| `R2010` | `AC1024` | AutoCAD 2010 |
| `R2013` | `AC1027` | AutoCAD 2013 |
| `R2018` | `AC1032` | AutoCAD 2018 |

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `String versionString()` | "AC1018" 등 헤더 문자열 반환 |
| `boolean isAtLeast(DwgVersion other)` | ordinal 비교 |
| `boolean isR2007OrLater()` | this >= R2007 (LZ77 압축, UTF-16 적용 여부) |
| `boolean isR2004OrLater()` | this >= R2004 (섹션 맵 구조 분기) |
| `boolean usesUnicode()` | isR2007OrLater() 와 동일 |
| `static DwgVersion fromString(String s)` | "AC1018" → R2004, 미지원시 DwgVersionException |

---

### `DwgVersionDetector`

파일 첫 6바이트를 읽어 DwgVersion을 판별.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static DwgVersion detect(byte[] headerBytes)` | 0~5바이트를 ASCII 문자열로 읽어 DwgVersion.fromString() 호출 |
| `static DwgVersion detect(Path filePath)` | 파일을 열어 첫 6바이트만 읽은 뒤 위임 |
| `static boolean isDwgFile(byte[] headerBytes)` | "AC10" 로 시작하는지 확인 |

---

## 패키지: `io.dwg.core.util`

---

### `CrcCalculator` *(abstract)*

CRC 계산 기반 클래스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `abstract int calculate(byte[] data, int seed)` | data에 대해 seed 기반 CRC 계산 |
| `abstract boolean verify(byte[] data, int expectedCrc)` | 계산값과 기대값 비교 |

---

### `Crc32Calculator`

R2004+ 파일 헤더/섹션 CRC32 검증에 사용.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `int calculate(byte[] data, int seed)` | seed XOR CRC32(data) 계산 |
| `int calculateForSection(byte[] data)` | seed=0으로 고정한 섹션 CRC 계산 |

---

### `Crc8Calculator`

R13-R15 오브젝트 맵 CRC8 검증에 사용.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `int calculate(byte[] data, int seed)` | 스펙 §23 table 기반 CRC8 계산 |

---

### `SentinelValidator`

섹션 경계의 16바이트 Sentinel 검증.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static void validate(byte[] actual, byte[] expected, String context)` | 불일치 시 DwgCorruptedException 발생 |
| `static byte[] invertSentinel(byte[] sentinel)` | 각 바이트 비트 반전 (end sentinel 생성에 사용) |
| `static byte[] HEADER_SENTINEL` | §3 헤더 시작 sentinel 상수 |

---

### `Lz77Decompressor`

R2007+ 섹션 데이터 압축 해제. 스펙 §5의 압축 알고리즘 구현.

**필드**
- `static final int WINDOW_SIZE = 0x8000` — 슬라이딩 윈도우 32KB

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `byte[] decompress(byte[] compressed, int expectedSize)` | 압축 데이터를 expectedSize 크기로 해제 |
| `private int readOpcode(BitInput in)` | 1바이트 opcode 읽기 |
| `private void handleLiteralRun(BitInput in, int count, byte[] out, int outPos)` | literal 바이트 그대로 복사 |
| `private void handleBackRef(int offset, int length, byte[] out, int outPos)` | 슬라이딩 윈도우 내 역참조 복사 |

---

### `Lz77Compressor`

R2007+ 섹션 데이터 압축. Decompressor의 역방향.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `byte[] compress(byte[] raw)` | raw 데이터를 LZ77로 압축 |
| `private int findLongestMatch(byte[] data, int pos, int windowStart)` | 슬라이딩 윈도우에서 가장 긴 매칭 탐색 |

---

### `DwgStringDecoder`

DWG 문자열 디코딩 유틸리티.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static String decode(byte[] bytes, DwgVersion ver)` | R2007 미만=CodePage 기반, R2007+=UTF-16LE 디코딩 |
| `static String decodeCodePage(byte[] bytes, int codePage)` | 지정 코드페이지로 디코딩 |
| `static Charset charsetForCodePage(int codePage)` | DWG 코드페이지 번호 → Java Charset 매핑 |

---

### `DwgStringEncoder`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static byte[] encode(String text, DwgVersion ver)` | 버전에 맞는 인코딩으로 변환 |
| `static byte[] encodeUtf16Le(String text)` | UTF-16LE 변환 |

---

## 패키지: `io.dwg.core.exception`

---

### `DwgParseException`

파싱 중 발생하는 checked 예외.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgParseException(String message)` | 메시지만 포함 |
| `DwgParseException(String message, Throwable cause)` | 원인 예외 체이닝 |
| `DwgParseException(String message, long bitOffset)` | 발생 위치(비트 오프셋) 포함 |
| `long bitOffset()` | 예외 발생 위치 반환 |

---

### `DwgWriteException`

쓰기 중 발생하는 checked 예외. 생성자 구조는 DwgParseException과 동일.

---

### `DwgVersionException`

지원하지 않는 버전의 파일을 읽을 때 발생.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgVersionException(String versionString)` | "Unsupported DWG version: AC9999" 형식 메시지 |
| `String detectedVersion()` | 감지된 버전 문자열 반환 |

---

### `DwgCorruptedException`

파일 손상(Sentinel 불일치, CRC 실패 등) 시 발생.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgCorruptedException(String context, String detail)` | 손상 위치와 상세 정보 포함 |

---

### `UnknownSectionException`

알 수 없는 섹션 이름 조회 시 발생. `UnknownEntityPolicy.THROW` 설정 시 사용.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `UnknownSectionException(String sectionName)` | 섹션명 포함 메시지 |
| `String sectionName()` | 알 수 없는 섹션 이름 반환 |

---

---

# 상세 설계 보충

---

## §2 비트 타입 인코딩 레퍼런스

### 전체 타입 표

| 타입 기호 | 이름 | 크기 | 인코딩 규칙 |
|---|---|---|---|
| `B` | Bit | 1 bit | raw bit (MSB 순서) |
| `BB` | 2-Bit | 2 bits | 2 raw bits, MSB first |
| `3B` | 3-Bit | 3 bits | 3 raw bits |
| `4B` | 4-Bit | 4 bits | 4 raw bits |
| `RC` | Raw Char | 8 bits | raw byte (부호 없음) |
| `RS` | Raw Short | 16 bits | little-endian unsigned short |
| `RL` | Raw Long | 32 bits | little-endian signed long |
| `RD` | Raw Double | 64 bits | little-endian IEEE 754 double |
| `BS` | Bit Short | 2~18 bits | 2-bit opcode + 조건부 데이터 |
| `BL` | Bit Long | 2~34 bits | 2-bit opcode + 조건부 데이터 |
| `BLL` | Bit Long Long | 3~67 bits | 3-bit 바이트수 + 가변 데이터 |
| `BD` | Bit Double | 2~66 bits | 2-bit opcode + 조건부 데이터 |
| `BDWMD` | Bit Double w/Default | 2~66 bits | 2-bit opcode + default 기반 패치 |
| `MC` | Modular Char | 8~40 bits | 가변 길이, 부호 포함 |
| `MS` | Modular Short | 16~80 bits | 가변 길이, 2바이트 단위 |
| `T` | Text | 가변 | BS 길이 + RC 배열 (R2004 이하) |
| `TU` | Unicode Text | 가변 | BS 문자수 + UTF-16LE |
| `TV` | Variable Text | 가변 | 버전 분기: R2007+ → TU, 미만 → T |
| `2BD` | 2D Bit Double | 가변 | BD + BD (X, Y) |
| `3BD` | 3D Bit Double | 가변 | BD + BD + BD (X, Y, Z) |
| `2RD` | 2D Raw Double | 16 bytes | RD + RD (X, Y) |
| `3RD` | 3D Raw Double | 24 bytes | RD + RD + RD (X, Y, Z) |

---

### BS (Bit Short) 인코딩 상세

```
read 2 bits → opcode

opcode == 00:  read RS (2 bytes little-endian, unsigned short) → value
opcode == 01:  read RC (1 byte, unsigned) → value
opcode == 10:  value = 0  (추가 비트 없음)
opcode == 11:  value = 256  (추가 비트 없음)
```

쓰기 시 선택 기준:
- `value == 0` → opcode `10`
- `value == 256` → opcode `11`
- `0 < value ≤ 255` → opcode `01` + 1 byte
- 그 외 → opcode `00` + 2 bytes

---

### BL (Bit Long) 인코딩 상세

```
read 2 bits → opcode

opcode == 00:  read RL (4 bytes little-endian, signed int) → value
opcode == 01:  read RC (1 byte, unsigned) → value
opcode == 10:  value = 0  (추가 비트 없음)
opcode == 11:  value = 0  (미정의, 0으로 처리)
```

쓰기 시 선택 기준:
- `value == 0` → opcode `10`
- `0 < value ≤ 255` → opcode `01` + 1 byte
- 그 외 → opcode `00` + 4 bytes

---

### BLL (Bit Long Long) 인코딩 상세 (§2.4)

```
read 3 bits → byteCount  (0~7)
read byteCount bytes, little-endian → unsigned 64-bit value

byteCount == 0 → value = 0 (bytes 없음)
byteCount == 1 → value = data[0]
...
byteCount == 7 → value = data[0..6] little-endian 조합
```

쓰기: value가 표현에 필요한 최소 바이트 수를 byteCount로 쓰고, 그만큼 little-endian 저장.

---

### BD (Bit Double) 인코딩 상세 (§2.5)

```
read 2 bits → opcode

opcode == 00:  read RD (8 bytes IEEE 754 double little-endian) → value
opcode == 01:  value = 1.0  (추가 비트 없음)
opcode == 10:  value = 0.0  (추가 비트 없음)
opcode == 11:  (미정의)
```

쓰기 시 선택 기준:
- `value == 0.0` → opcode `10`
- `value == 1.0` → opcode `01`
- 그 외 → opcode `00` + 8 bytes

---

### BDWMD (Bit Double With Default) 인코딩 상세 (§2.9)

`readBitDoubleWithDefault(double def)`의 opcode 해석:

```
read 2 bits → opcode

opcode == 00:  read RD (8 bytes) → 완전히 새 값
opcode == 01:  value = def  (default 그대로, 추가 비트 없음)
opcode == 10:  read 4 bytes (RS×2) → XOR patch
               default의 raw bytes를 long-endian으로 해석하면서
               상위 4바이트는 default의 상위 4바이트 사용,
               하위 4바이트는 읽은 값으로 교체
opcode == 11:  read 4 bytes → 이 4바이트를 double 하위 4바이트로,
               상위 4바이트는 default 상위 4바이트 사용
```

---

### MC (Modular Char) 인코딩 상세 (§2.6)

**디코딩 의사코드:**

```
value    = 0
shift    = 0
negative = false
is_first = true

while true:
    b = readRawChar()
    has_more = (b & 0x80) != 0   // bit7: 뒤에 더 있음

    if is_first:
        negative = (b & 0x40) != 0  // bit6: 음수 플래그 (첫 바이트만)
        data    = b & 0x3F           // bits[5:0]: 6개 데이터 비트
        shift   = 6
        is_first = false
    else:
        data = b & 0x7F              // bits[6:0]: 7개 데이터 비트
        // shift는 이전 반복에서 누적됨

    value |= (data << shift)
    if not is_first_iter:
        shift += 7                   // 두 번째 이후 바이트는 7비트 스텝

    if not has_more: break

if negative: value = -value
return value
```

> 스펙 §2.6 검증 예시:
> | 값 | 인코딩 바이트 (hex) |
> |---|---|
> | `4610` | `22 E2 02` |
> | `-1413` | `C5 8B 00` (bit6=1 → 음수) |
> | `112823273` | `69 E9 83 AD 06` |

**인코딩 의사코드:**

```
is_negative = (value < 0)
if is_negative: value = -value

first = true
while true:
    if first:
        data  = value & 0x3F          // 하위 6비트
        value >>= 6
        b = data | (is_negative ? 0x40 : 0x00) | (value != 0 ? 0x80 : 0x00)
        first = false
    else:
        data  = value & 0x7F          // 하위 7비트
        value >>= 7
        b = data | (value != 0 ? 0x80 : 0x00)

    writeRawChar(b)
    if value == 0: break
```

---

### MS (Modular Short) 인코딩 상세 (§2.7)

```
// 디코딩
value = 0
shift = 0

while true:
    s = readRawShort()           // 2바이트 little-endian
    has_more = (s & 0x8000) != 0 // bit15: 더 있음
    data     = s & 0x7FFF         // bits[14:0]: 15비트 값

    value |= (data << shift)
    shift += 15
    if not has_more: break

return value
```

---

### Handle 인코딩 상세 (§2 Handle Reference)

**비트 레이아웃:**

```
[4 bits: code] [4 bits: counter] [counter bytes: handle value (MSB-first)]
```

counter = 0이면 handle value 없음 (0 또는 context 기반 추론).

**code 값 및 절대 핸들 계산 방법:**

| code | 이름 | 절대 핸들 계산 |
|------|------|--------------|
| `0x00` | 절대 | `value` |
| `0x01` | 소유자 상대 +1 | `ownerHandle + 1` (counter=0일 때) |
| `0x02` | 소유자 상대 -1 | `ownerHandle - 1` (counter=0일 때) |
| `0x03` | 소유자 상대 +n | `ownerHandle + value` |
| `0x04` | 소유자 상대 -n | `ownerHandle - value` |
| `0x05` | 상대 (soft ptr) | `lastHandle + value` (이전 핸들 기준 오프셋) |
| `0x06` | Soft 소유권 포인터 | `value` (절대) |
| `0x07` | Hard 소유권 포인터 | `value` (절대) |
| `0x08` | Soft 포인터 | `value` (절대) |
| `0x09` | Hard 포인터 | `value` (절대) |
| `0x0A` | Null/Owner (없음) | `0` |

**DwgHandle.resolve() 의사코드:**

```java
long resolve(long ownerHandle) {
    return switch (code) {
        case 0x00 -> value;
        case 0x01 -> ownerHandle + 1;
        case 0x02 -> ownerHandle - 1;
        case 0x03 -> ownerHandle + value;
        case 0x04 -> ownerHandle - value;
        case 0x06, 0x07, 0x08, 0x09 -> value;
        case 0x0A -> 0L;
        default   -> value;
    };
}
```

---

## LZ77 압축 알고리즘 상세 (§5, R2007+)

슬라이딩 윈도우 크기: **0x8000 (32,768 bytes)**

### Decompression opcode 해석 규칙

압축 데이터를 순차로 읽으며 opcode에 따라 두 가지 동작을 수행:

1. **Literal run**: 압축 스트림의 원시 바이트를 출력으로 그대로 복사
2. **Back-reference**: 출력 버퍼 내 이전 위치에서 N바이트를 현재 위치로 복사

**Opcode 바이트 해석:**

| Opcode 범위 | 명령 타입 | 길이 계산 | 오프셋 계산 |
|---|---|---|---|
| `0x00` | 스트림 종료 | — | — |
| `0x01`-`0x0F` | literal run 예비 (`0x0F` = 최대 literal 예비) | opcode & 0x0F = 추가 literal 카운터 | — |
| `0x10`-`0x1F` | long back-reference | `(opcode & 0x0F) << 8 + next_byte` + 3 | 다음 2바이트 = little-endian offset (+ 0x4000 보정) |
| `0x20`-`0xFF` | short back-reference + literal prefix | length = opcode 상위 nibble 기반 | offset = opcode 하위 nibble + next byte |

**구체적 의사코드:**

```
out_pos = 0
while in_pos < compressed.length:
    opcode = compressed[in_pos++]

    if opcode == 0x00:
        break  // end of stream

    // ① Literal run 처리
    // high nibble이 0x00이면 literal 길이 추가 인코딩
    if (opcode & 0xF0) == 0x00:
        length = opcode & 0x0F  // 0x01-0x0F
        if length == 0:
            // 확장 길이: 0x00 바이트들의 합 + 0x0F + 3
            while (ext = compressed[in_pos++]) == 0x00:
                length += 0xFF
            length += ext + 0x0F + 3
        else:
            length += 3
        copy(compressed, in_pos, out, out_pos, length)
        in_pos  += length
        out_pos += length
        continue

    // ② Back-reference (길이/오프셋 추출)
    // 상위 nibble로 명령 분류
    high = opcode >> 4
    low  = opcode & 0x0F

    if high == 0x1:
        // long back-reference
        next_byte = compressed[in_pos++]
        length    = (low << 8 | next_byte) + 3
        offset    = (compressed[in_pos] | compressed[in_pos+1] << 8) + 1
        in_pos   += 2
    else:
        // short back-reference
        // 상위 nibble: length hint, 하위 nibble + 다음 바이트: offset
        length  = high + 1  // (실제 공식은 스펙 표 참조)
        next_byte = compressed[in_pos++]
        offset  = (low << 8 | next_byte) + 1

    // 슬라이딩 윈도우 내에서 복사 (overlap 허용)
    src = out_pos - offset
    for i in 0..length-1:
        out[out_pos++] = out[src + i]
```

> **주의:** offset이 length보다 작으면 반복 패턴 복사가 된다 (run-length-like). 이 경우도 byte-by-byte 복사로 올바르게 처리된다.

---

## CRC 알고리즘 상세

### CRC-16 (Object Map, §23)

스펙 §23의 핸들 맵 섹션 끝에 2바이트 CRC가 붙는다.
다항식: **0xA001** (CRC-16/IBM, reversed polynomial)

```
static final int[] CRC16_TABLE = precompute(0xA001);
// 256-entry table, 각 entry = CRC of single byte

int crc16(byte[] data, int seed) {
    int crc = seed & 0xFFFF;
    for (byte b : data) {
        crc = (crc >> 8) ^ CRC16_TABLE[(crc ^ b) & 0xFF];
    }
    return crc & 0xFFFF;
}
```

Object Map 블록 CRC 계산:
- seed = `0xC0C1`
- data = 블록 헤더(섹션 크기 RS) + 핸들 엔트리 전체 바이트
- CRC를 블록 끝 2바이트(RS)에 little-endian으로 기록

### CRC-32 (R2004+ File Header / Section Data)

표준 CRC-32/ISO-HDLC (ZIP 방식).
다항식: **0xEDB88320** (reflected)

```
int crc32(byte[] data, int seed) {
    int crc = seed ^ 0xFFFFFFFF;
    for (byte b : data) {
        crc = (crc >>> 8) ^ CRC32_TABLE[(crc ^ b) & 0xFF];
    }
    return (crc ^ 0xFFFFFFFF);
}
```

R2004 파일 헤더 검증:
- CRC 필드(오프셋 0x08, 4바이트)를 0으로 마스킹
- 헤더 전체 0x80바이트에 대해 seed=0으로 계산
- 계산값과 필드값 비교

---

## Sentinel 상수 정의

스펙 §3/§9 각 섹션 경계의 16바이트 Sentinel:

| 섹션 | 시작 Sentinel (hex) |
|---|---|
| Header Variables 시작 | `CF 7B 1F 23 FD DE 38 A9 5F 7C 68 B8 4E 6D 33 5F` |
| Header Variables 끝 | `30 84 E0 DC 02 21 C7 56 A0 83 97 47 B1 92 CC A0` (시작의 비트 반전) |
| Classes 시작 | `8D A1 C4 B8 C4 A9 F8 C5 C0 DC F4 5F E7 CF B6 8A` |
| Classes 끝 | 시작 Sentinel의 각 바이트 비트 반전 |
| Second Header 시작 | `D4 7B 21 CE 28 93 9F BF 53 24 40 09 12 3C AA 01` |

`SentinelValidator.invertSentinel()`: 각 바이트에 `~b` 적용하여 end sentinel 생성.

---

## DwgStringDecoder 코드페이지 매핑 (주요)

| DWG codePage 번호 | Java Charset |
|---|---|
| 30 | `windows-1252` (Western Europe) |
| 29 | `windows-1251` (Cyrillic) |
| 28 | `windows-1250` (Central Europe) |
| 0 | `US-ASCII` |
| 25 | `EUC-KR` (Korean) |
| 26 | `EUC-JP` (Japanese) |
| R2007+ | UTF-16LE (항상, codePage 무관) |
