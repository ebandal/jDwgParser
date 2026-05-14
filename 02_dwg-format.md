# dwg-format — 클래스 명세

> 버전별 파일 구조 핸들러. 스펙 §3(R13-R15), §4(R2004), §5(R2007), §6(R2010), §7(R2013), §8(R2018) 구현.  
> 책임: 파일을 열어 섹션 바이트 배열을 추출하고, 각 섹션을 `SectionInputStream`으로 제공하는 것까지.

---

## 패키지: `io.dwg.format`

---

### `DwgFileStructureHandler` *(interface)*

버전별 파일 구조 처리기의 계약. Strategy 패턴의 전략 인터페이스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgVersion version()` | 이 핸들러가 담당하는 DWG 버전 반환 |
| `FileHeaderFields readHeader(BitInput input)` | 파일 헤더 파싱 후 공통 헤더 필드 반환 |
| `Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)` | 섹션명 → 원시 바이트 맵 구성 |
| `void writeHeader(BitOutput output, FileHeaderFields header)` | 헤더를 파일에 씀 |
| `void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header)` | 각 섹션 바이트를 파일 구조에 맞게 씀 |
| `boolean supports(DwgVersion version)` | 이 핸들러가 해당 버전을 처리할 수 있는지 |

---

### `AbstractFileStructureHandler` *(abstract)*

공통 로직 (CRC 검증 호출, Sentinel 체크) 를 제공하는 기반 클래스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `protected void validateSentinel(byte[] actual, byte[] expected, String ctx)` | SentinelValidator 위임, 실패 시 DwgCorruptedException |
| `protected void validateCrc(byte[] data, int expected, String ctx)` | CRC 검증, 실패 시 DwgCorruptedException |
| `protected byte[] readBytes(BitInput input, int count)` | count 바이트 순차 읽기 편의 메서드 |
| `protected void writeBytes(BitOutput output, byte[] data)` | 바이트 배열 순차 쓰기 편의 메서드 |

---

### `DwgFileStructureHandlerFactory`

`DwgVersion`에 맞는 `DwgFileStructureHandler` 인스턴스를 반환하는 팩토리.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static DwgFileStructureHandler forVersion(DwgVersion version)` | version switch → 적합한 Handler 인스턴스 반환. 미지원 시 DwgVersionException |
| `static DwgFileStructureHandler detect(byte[] fileBytes)` | 파일 헤더에서 버전 자동 감지 후 위임 |

---

## 패키지: `io.dwg.format.common`

---

### `FileHeaderFields`

모든 버전에서 공통으로 추출하는 헤더 정보. 버전별 차이를 흡수하는 DTO.

**필드**
- `DwgVersion version`
- `int maintenanceVersion` — 마이너 버전
- `long previewOffset` — 미리보기 섹션 오프셋
- `int codePage` — 문자 코드페이지
- `int securityFlags` — 암호화 플래그 (R2004+)
- `long summaryInfoOffset` — SummaryInfo 오프셋 (R2004+)
- `long vbaProjectOffset` — VBA 프로젝트 오프셋 (R2004+)
- `Map<String, Long> sectionOffsets` — 섹션명 → 파일 오프셋 (R13-R15용)

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `FileHeaderFields(DwgVersion version)` | version 설정, 나머지 기본값 초기화 |
| `boolean isEncrypted()` | securityFlags & 0x01 != 0 |
| `Optional<Long> sectionOffset(String name)` | 섹션명으로 오프셋 조회 |

---

### `SectionDescriptor`

R2004+ 섹션 맵 내의 각 섹션 기술자 정보.

**필드**
- `String name` — 섹션 이름 (예: "AcDb:Header")
- `long compressedSize`
- `long uncompressedSize`
- `int compressionType` — 0=none, 2=LZ77
- `List<PageInfo> pages` — 이 섹션을 구성하는 페이지 목록

---

### `PageInfo`

파일 내 데이터 페이지 위치와 크기 정보.

**필드**
- `long pageOffset` — 파일 내 절대 오프셋
- `long dataSize` — 페이지 데이터 크기
- `long pageId`

---

### `SectionType` *(enum)*

알려진 DWG 섹션 이름 상수.

| 상수 | 값 |
|---|---|
| `HEADER` | `"AcDb:Header"` |
| `CLASSES` | `"AcDb:Classes"` |
| `HANDLES` | `"AcDb:Handles"` |
| `OBJECTS` | `"AcDb:AcDbObjects"` |
| `SUMMARY_INFO` | `"AcDb:SummaryInfo"` |
| `PREVIEW` | `"AcDb:Preview"` |
| `APP_INFO` | `"AcDb:AppInfo"` |
| `FILE_DEP_LIST` | `"AcDb:FileDepList"` |
| `REV_HISTORY` | `"AcDb:RevHistory"` |
| `SECURITY` | `"AcDb:Security"` |
| `OBJ_FREE_SPACE` | `"AcDb:ObjFreeSpace"` |
| `TEMPLATE` | `"AcDb:Template"` |
| `AUX_HEADER` | `"AcDb:AuxHeader"` |

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `String sectionName()` | 문자열 이름 반환 |
| `static Optional<SectionType> fromName(String name)` | 이름으로 enum 조회 |

---

## 패키지: `io.dwg.format.r13`

---

### `R13FileStructureHandler`

스펙 §3 (R13-R15 DWG FILE FORMAT ORGANIZATION) 구현.  
R13과 R14는 동일 구조이므로 하나의 핸들러로 처리.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgVersion version()` | `DwgVersion.R13` 반환 (R14도 처리 가능) |
| `boolean supports(DwgVersion v)` | v == R13 \|\| v == R14 |
| `FileHeaderFields readHeader(BitInput input)` | §3: 버전문자열(6B)+unknown(6B)+codePage(2B)+섹션수(RL)+섹션 locator 배열 읽기 |
| `Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)` | 각 섹션 locator의 오프셋/크기로 바이트 추출 |
| `void writeHeader(BitOutput output, FileHeaderFields header)` | 역순 직렬화 |
| `void writeSections(BitOutput output, Map<String, byte[]> sections, FileHeaderFields header)` | 섹션 오프셋 계산 후 직렬화 |

---

### `R13FileHeader`

R13-R15 파일 헤더 원시 데이터.

**필드**
- `String versionString` — "AC1012" / "AC1014"
- `byte[] unknown6` — 알 수 없는 6바이트
- `int codePage`
- `int sectionCount`

---

### `R13SectionLocator`

R13-R15의 섹션 위치 기술자. 레코드 1개 = 섹션 1개.

**필드**
- `int recordNumber` — 섹션 번호 (0=헤더변수, 1=클래스 등)
- `long seeker` — 파일 내 절대 오프셋 (RL)
- `long size` — 섹션 크기 (RL)

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R13SectionLocator read(BitInput input)` | recordNumber(RL) + seeker(RL) + size(RL) 읽기 |
| `void write(BitOutput output)` | 역순 직렬화 |
| `String toSectionName()` | recordNumber → SectionType 이름 매핑 |

---

### `R13SecondFileHeader`

§26 SECOND FILE HEADER (R13-R15). 파일 끝 부분에 위치하는 중복 헤더.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R13SecondFileHeader read(BitInput input, long offset)` | 지정 오프셋으로 seek 후 파싱. 핸들 섹션 오프셋 재확인에 사용 |
| `long handlesOffset()` | Object Map 오프셋 반환 |

---

## 패키지: `io.dwg.format.r2004`

---

### `R2004FileStructureHandler`

스펙 §4 (R2004 DWG FILE FORMAT ORGANIZATION) 구현.  
R2000도 호환 가능 (시스템 섹션 구조 동일).

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean supports(DwgVersion v)` | v == R2000 \|\| v == R2004 |
| `FileHeaderFields readHeader(BitInput input)` | §4: 파일 헤더 0x80바이트 읽기, XOR 복호화, CRC 검증 |
| `Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)` | Section Map 페이지 → Data Section Descriptor → 각 섹션 바이트 조합 |
| `private R2004SectionMap readSectionMap(BitInput input, long offset)` | Section Map 위치로 seek 후 파싱 |
| `private byte[] assembleSectionData(BitInput input, SectionDescriptor desc)` | 여러 페이지의 데이터를 순서대로 읽어 합치고 압축 해제 |

---

### `R2004FileHeader`

R2004 파일 헤더 0x80바이트 구조.

**필드**
- `String versionString`
- `byte[] unknown1` — 5바이트
- `int maintenanceVersion`
- `int previewOffset`
- `int codePage`
- `int securityFlags`
- `int unknownLong`
- `int summaryInfoOffset`
- `long vbaProjectOffset`
- `int rootTreeNodeGap`
- `int lowerLeftGap`
- `int lowerRightGap`
- `int upperLeftGap`
- `int upperRightGap`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2004FileHeader read(BitInput input)` | 0x80바이트 읽기, §4의 XOR 복호화 (키=0x4848...) 후 CRC 검증 |
| `void write(BitOutput output)` | 역순 직렬화 + XOR 암호화 + CRC 계산 |

---

### `R2004SectionMap`

섹션 맵 페이지 파싱. 섹션 이름과 페이지 목록을 구성.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2004SectionMap read(BitInput input, long pageMapOffset)` | 페이지 맵 위치로 seek, 섹션 수(RL) 읽기, 각 SectionDescriptor 파싱 |
| `List<SectionDescriptor> descriptors()` | 전체 섹션 기술자 목록 |
| `Optional<SectionDescriptor> find(String name)` | 이름으로 섹션 기술자 조회 |

---

### `R2004DataSectionDescriptor`

각 데이터 섹션의 메타 정보 (이름, 압축 타입, 페이지 목록).

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2004DataSectionDescriptor read(BitInput input)` | §4 Data Section Map 엔트리 파싱: 섹션명 길이(BL)+이름+크기정보+페이지수+페이지목록 |

---

### `R2004PageDescriptor`

페이지 위치와 크기.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2004PageDescriptor read(BitInput input)` | pageId(RL)+dataSize(RL)+offset(RL) 읽기 |

---

### `R2004SystemSectionManager`

Section Map, Page Map 등 시스템 섹션 관리.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `long locateSectionMapPage(R2004FileHeader header, BitInput input)` | 헤더의 sectionMapId를 기반으로 Section Map 페이지 위치 산출 |
| `long locatePageMapOffset(R2004FileHeader header)` | 헤더에서 Page Map 오프셋 추출 |

---

## 패키지: `io.dwg.format.r2007`

---

### `R2007FileStructureHandler`

스펙 §5 (R2007 DWG FILE FORMAT ORGANIZATION) 구현. 가장 복잡한 핸들러.  
LZ77 압축, UTF-16 문자열, 서명 섹션 처리.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean supports(DwgVersion v)` | v == R2007 \|\| v == R2010 |
| `FileHeaderFields readHeader(BitInput input)` | §5: 파일 헤더 파싱. 암호화된 헤더 복호화 포함 |
| `Map<String, SectionInputStream> readSections(BitInput input, FileHeaderFields header)` | Page Map → Section Map → 각 섹션 페이지 조합 + LZ77 해제 |
| `private R2007PageMap readPageMap(BitInput input, long offset)` | 페이지 맵 섹션 파싱 |
| `private byte[] readAndDecompress(BitInput input, PageInfo page, boolean compressed)` | 페이지 읽기 + LZ77 해제 조건 분기 |

---

### `R2007FileHeader`

R2007 파일 헤더 구조. §5.2 파싱.

**필드**
- `byte[] headerData` — 0x70바이트 헤더 원시 데이터
- `long pageMapOffset`
- `long sectionMapId`
- `long gap1`, `long gap2` — 오프셋 계산용

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2007FileHeader read(BitInput input)` | §5.2 헤더 파싱. 복잡한 XOR 복호화 (키 스케줄 포함) |
| `void write(BitOutput output)` | 역순 암호화 + 직렬화 |

---

### `R2007PageMap`

R2007 페이지 맵. 페이지 ID → 파일 오프셋 매핑.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2007PageMap read(BitInput input, long offset, long size)` | LZ77 해제 후 페이지 ID와 오프셋 쌍 파싱 |
| `Optional<Long> offsetForPage(long pageId)` | 페이지 ID로 오프셋 조회 |
| `List<Long> pageIds()` | 전체 페이지 ID 목록 |

---

### `R2007SectionMap`

R2007 섹션 맵. 섹션명 → 페이지 목록 매핑.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2007SectionMap read(BitInput input, byte[] decompressedData)` | §5.4 파싱: 섹션 수 + 각 SectionDescriptor |
| `List<SectionDescriptor> descriptors()` | 전체 섹션 기술자 반환 |
| `Optional<SectionDescriptor> find(String name)` | 이름으로 조회 |

---

### `R2007CompressionHandler`

R2007 섹션별 LZ77 압축/해제 처리.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `byte[] decompress(byte[] compressed, long expectedSize)` | Lz77Decompressor 위임, 크기 검증 |
| `byte[] compress(byte[] raw)` | Lz77Compressor 위임 |
| `boolean isCompressed(SectionDescriptor desc)` | desc.compressionType == 2 여부 |

---

### `R2007SignatureSection`

§5의 서명/보안 섹션 파싱.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2007SignatureSection read(BitInput input, long offset)` | 서명 섹션 읽기. 현재는 skip 처리 가능 |
| `boolean hasSignature()` | 서명 존재 여부 |

---

### `R2007EncryptionHandler`

R2007 헤더 암호화/복호화.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `byte[] decrypt(byte[] encryptedHeader)` | §5.2 XOR 키 스케줄로 복호화 |
| `byte[] encrypt(byte[] plainHeader)` | 역방향 암호화 |
| `private int[] generateKeySchedule(byte[] seed)` | 키 스케줄 생성 (Vernam-like XOR) |

---

## 패키지: `io.dwg.format.r2013`

---

### `R2013FileStructureHandler`

스펙 §7 (R2013). R2007 구조에서 최소 변경. R2007Handler 상속 또는 위임으로 구현.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean supports(DwgVersion v)` | v == R2013 |
| `FileHeaderFields readHeader(BitInput input)` | R2007 헤더 파싱 + R2013 추가 필드 처리 |

---

### `R2018FileStructureHandler`

스펙 §8 (R2018). R2013과 동일 구조. 버전 문자열만 다름.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean supports(DwgVersion v)` | v == R2018 |

---

### `R2013FileHeader`

R2013/R2018 파일 헤더. R2007 헤더와 거의 동일하나 일부 필드 위치 변경.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static R2013FileHeader read(BitInput input)` | §7 기준 파싱 |

---

---

# 상세 설계 보충

---

## R13/R14 파일 구조 상세 (§3)

### 파일 레이아웃 (바이트 오프셋 기준)

```
Offset  Size  Type  설명
------  ----  ----  ----
0x0000     6   RC   버전 문자열 ("AC1012" 또는 "AC1014")
0x0006     6   RC   미사용 (unknown bytes)
0x000C     2   RS   코드페이지 번호 (little-endian)
0x000E     4   RL   섹션 수 (N)
0x0012  N×12  ---   섹션 Locator 배열 (아래 참조)

// 섹션 Locator 1개 = 12 bytes
  +0  4  RL  레코드 번호 (0~4)
  +4  4  RL  seeker (파일 내 섹션 데이터 절대 오프셋)
  +8  4  RL  size (섹션 크기, bytes)
```

**레코드 번호 → 섹션 이름 매핑:**

| 레코드 번호 | 섹션 이름 | 내용 |
|---|---|---|
| 0 | `AcDb:Header` | 헤더 변수 (§9) |
| 1 | `AcDb:Classes` | 클래스 정의 (§10) |
| 2 | `AcDb:Handles` | 오브젝트 맵 (§23) |
| 3 | (reserved) | 미사용 |
| 4 | `AcDb:AuxHeader` | 보조 헤더 (§27, R14 전용) |

### 각 섹션의 내부 구조

모든 R13/R14 섹션은 아래 패턴:
```
[Section Start Sentinel: 16 bytes]
[Section Data Size: RL, 4 bytes]
[Section Data: Size bytes]
[Section CRC-16: RS, 2 bytes]
[Section End Sentinel: 16 bytes]  ← Start Sentinel 비트 반전
```

### R13 Second File Header (§26)

파일 끝 근처에 위치하는 중복 헤더. Object Map 오프셋 재확인에 사용.

```
Offset  Size  Type  설명
------  ----  ----  ----
0        2   RS    섹션 크기 (가변)
2        4   RL    파일 내 ofsset: Header Variables
6        4   RL    파일 내 offset: Object Map
10       1   RC    섹션 수
...      (버전 필드, 핸들 정보 등)
```

---

## R2000 파일 구조 상세 (§3 변형 + §10/§23/§20)

**⚠️ R2000은 R13/R14의 섹션 구조를 그대로 사용하지만, Locator 해석이 다름**

### 파일 레이아웃

```
Offset      크기      설명
----------  --------  ----
0x0000-     ~94B      헤더 (R13과 동일: 버전문자열 + 미사용 + 코드페이지 + Locators)
0x005E-     variable  Objects 섹션 (Classes + Handles + Objects 모두 포함)
0x6B8B-     518B      Header Variables 섹션 (Locator[0]이 지시)
```

### R2000 Locator 구조

R13/R14와 동일 (12바이트 × N):
```
[RL] record_number   (0 = Header, 1 = unused, 2 = unused, 3 = unused, ...)
[RL] seeker         (파일 내 섹션 오프셋)
[RL] size           (섹션 크기)
```

**핵심 차이점:** R13/R14에서는 locator[0]=Header, [1]=Classes, [2]=Handles, [3]=Objects인데,
**R2000에서는 locator[0]=Header, [1+]=Objects(합침)**

### Objects 섹션 내부 구조

한 섹션에 Classes + Handles + Objects가 **순차적으로 배치**:

#### 1️⃣ Classes 데이터 (R13/R14 형식, Sentinel 포함)
```
[16B]      Start Sentinel
[RL 4B]    Data Size
[N bytes]  Classes entries:
           - classNum (BS)
           - version (BS)
           - appName (TV)
           - cppName (TV)
           - dxfName (TV)
           - wasZombie (B)
           - isEntity (BS)
           ... 반복 ...
[RS 2B]    CRC-16 (seed=0xC0C1)
[16B]      End Sentinel (Start 비트반전)
```

#### 2️⃣ Handles 데이터 (페이지 기반, R13/R14 형식)
```
Loop (각 페이지):
  [RS_BE 2B]      page_size (big-endian! ⚠️ 2032-2040 typical)
  [UMC 1-3B]      handle_delta (누적)
  [MC 1-3B]       offset_delta (누적, signed)
  [...]           repeat until page_size reached
  [RS_BE 2B]      crc (big-endian, seed=0xC0C1)

Until: page_size == 2 (종료 신호)
```

**⚠️ 중요:**
- `page_size`는 **RS_BE (big-endian)** - 가장 중요한 바이트가 먼저
- `handle_delta` = UMC (Unsigned Modular Char, 항상 양수)
- `offset_delta` = MC (Signed Modular Char, 음수 가능, 누적)
- CRC는 각 페이지별로 계산

#### 3️⃣ Objects 데이터 (offset 0부터 시작)
```
[MS 2+ B]       objectSize
[BS 2B]         typeCode  
[H variable]    objectHandle
[BS 2B]         xDataSize
[XData var]     xData (if xDataSize > 0)
... 공통 헤더 ...
... 타입별 데이터 ...

다음 객체는 바로 이어짐 (패딩 없음)
```

**핵심:** Handles 섹션에서 생성된 offset 맵으로 각 객체 위치 파악

### R2000 vs R13/R14 비교

| 항목 | R13/R14 | R2000 |
|-----|---------|-------|
| 섹션 배치 | 별도 locator로 구분 | 합쳐짐 (동일 offset) |
| Classes | 별도 섹션 | Objects 내 (Sentinel) |
| Handles | 별도 섹션 | Objects 내 (페이지형) |
| Objects | 별도 섹션 | Objects 내 (offset 0) |
| 파싱 순서 | locator → section | section 내 순차추출 → handle map → objects |

### R2000 vs R2004 비교

| 항목 | R2000 | R2004 |
|-----|-------|-------|
| 압축 | 없음 | LZ77 (선택) |
| 섹션구조 | 고정 (locator) | 동적 (섹션맵) |
| Handle 포맷 | RS_BE pages | 동일 (RS_BE pages) |
| Object 포맷 | offset 0부터 | offset 0부터 (동일) |

---

## R2004 파일 구조 상세 (§4)

### 파일 헤더 0x80바이트 레이아웃

오프셋 0x00~0x7F의 128바이트. XOR 암호화되어 있음 → 복호화 후 파싱.

```
Offset  Size  Type  설명
------  ----  ----  ----
0x00       6   RC   버전 문자열 ("AC1018")
0x06       1   RC   미사용
0x07       1   RC   maintenance version
0x08       4   RL   CRC-32 (이 필드 자체는 계산 시 0으로 설정)
0x0C       1   RC   0x00
0x0D       1   RC   Application version (optional)
0x0E       2   RS   코드페이지 번호
0x10       3   RC   미사용
0x13       1   RC   Security flags 하위 바이트
0x14       3   RC   미사용
0x17       1   RC   Security flags 상위 바이트
0x18       4   RL   SummaryInfo 페이지 오프셋
0x1C       4   RL   VBA project 오프셋 (없으면 0)
0x20       4   RL   0x00000080 (항상 이 값, 헤더 크기 지시)
0x24       4   RL   Root tree node gap
0x28       4   RL   Lowermost left tree node gap
0x2C       4   RL   Lowermost right tree node gap
0x30       4   RL   Upper left tree node gap
0x34       4   RL   Upper right tree node gap
0x38       4   RL   Number of segments (Section Map의 페이지 수)
0x3C       8   RD   미사용
0x44       4   RL   Section Page Map ID (system section)
0x48       8   RD   미사용
0x50       4   RL   Section Map ID (data section map)
0x54       4   RL   Section Page Array Size
0x58       4   RL   Gap Amount
0x5C       4   RL   Section Location Count
0x60      12   RC   미사용
0x6C       4   RL   Page Count
0x70       2   RS   미사용
0x72       2   RS   섹션 맵 ID (check)
0x74       4   RL   Page Map Offset (파일 내 절대 오프셋)
0x78       4   RL   미사용
0x7C       4   RL   미사용
```

### R2004 헤더 XOR 복호화 알고리즘

```
// 헤더 0x80바이트는 XOR 키로 암호화되어 있음
// 키: 고정 4바이트 키 0x4848454C을 반복 적용

static final byte[] XOR_KEY = {0x4C, 0x45, 0x48, 0x48};  // "LEHH" (little-endian)

byte[] decryptHeader(byte[] encrypted) {
    byte[] decrypted = new byte[0x80];
    for (int i = 0; i < 0x80; i++) {
        decrypted[i] = (byte)(encrypted[i] ^ XOR_KEY[i & 3]);
    }
    // 복호화 후 오프셋 0x08의 CRC-32 필드를 0으로 채우고
    // 전체 0x80 바이트에 대한 CRC-32 계산 → 0x08 필드 값과 비교
    return decrypted;
}
```

### R2004 Section Map 파싱 알고리즘

**1단계: Page Map 읽기**

Page Map = 파일 내 모든 페이지의 (ID → 오프셋) 매핑 테이블.

```
위치: FileHeader.pageMapOffset
크기: (각 섹션 크기 합산으로 계산)

페이지 맵 구조 (LZ77 압축 없음, raw):
  반복:
    pageId   (RL, 4 bytes)  -- 양수=데이터, 음수=여유 공간
    pageSize (RL, 4 bytes)  -- 페이지 크기
  파일 오프셋은 0x100에서 시작해 각 pageSize만큼 누적
```

**2단계: Section Map 읽기**

Section Map = 논리 섹션 이름과 물리 페이지 목록의 매핑.

```
위치: FileHeader.sectionMapId 페이지의 파일 오프셋 (PageMap에서 조회)
압축: 없음 (system section은 비압축)

구조:
  numDescriptors (RL, 4 bytes)
  반복 numDescriptors번:
    dataSize       (RL, 8 bytes)  -- uncompressed size
    maxDecompSize  (RL, 8 bytes)  -- max decompressed page size
    unknown        (RL, 4 bytes)
    compressionType(RL, 4 bytes)  -- 1=uncompressed, 2=LZ77
    sectionType    (RL, 4 bytes)  -- 0=unprotected, 1=protected, 2=compressed
    pageCount      (RL, 4 bytes)
    padding        (2 bytes)
    nameLen        (BS)
    name           (RC × nameLen)  -- UTF-8 섹션 이름
    반복 pageCount번:
      pageId   (RL, 4 bytes)
      dataSize (RL, 4 bytes)    -- compressed size in this page
      offset   (RL, 8 bytes, 반올림)
```

**3단계: 섹션 데이터 조합**

```
for each section descriptor:
    rawBytes = []
    for each page in descriptor.pages:
        fileOffset = pageMap.offsetFor(page.pageId)
        pageData = fileBytes[fileOffset .. fileOffset + page.dataSize]
        if descriptor.compressionType == 2:
            pageData = LZ77Decompress(pageData, maxDecompSize)
        rawBytes.append(pageData)
    // rawBytes를 SectionInputStream으로 래핑
```

---

## R2007 파일 구조 상세 (§5)

### 파일 헤더 구조 (§5.2)

```
Offset  Size  Type  설명
------  ----  ----  ----
0x00       6   RC   버전 문자열 ("AC1021")
0x06       7   RC   미사용
0x0D       1   RC   maintenance version
0x0E       2   RC   미사용
0x10       4   RL   Preview offset (file offset to thumbnail)
0x14       1   RC   Application version
0x15       1   RC   maintenance version (app)
0x16       2   RS   코드페이지
0x18       3   RC   미사용
0x1B       1   RC   Security flags
0x1C       4   RL   Summary info address (System section)
0x20       4   RL   VBA project offset
0x24       4   RL   0x00000080
0x28      54   RC   미사용 (encrypted header follows separately)
0x5E      32   RC   암호화된 헤더 데이터 (§5.2 참조)

// 0x5E~0x7D: 32바이트 encrypted header (별도 복호화 필요)
```

### R2007 헤더 복호화 알고리즘 (§5.2)

파일 오프셋 0x5E의 32바이트를 XOR 키 스케줄로 복호화:

```
// 키 시드: 파일 오프셋 0x00~0x0D (14바이트)
byte[] seed = fileBytes[0x00..0x0D]

// 키 스케줄 생성 (Vernam-like)
int[] keySchedule = new int[4];
keySchedule[0] = seed[0x00..0x03] as RL;
keySchedule[1] = seed[0x04..0x07] as RL;
keySchedule[2] = seed[0x08..0x0B] as RL;
keySchedule[3] = seed[0x0C] | (seed[0x0D] << 8) | 0xA502_0000;

// 암호화된 32바이트 복호화
byte[] encryptedHeader = fileBytes[0x5E..0x7D]
byte[] decryptedHeader = new byte[32]
for i in 0..7:
    int block = encryptedHeader[i*4..i*4+3] as RL
    int key   = keySchedule[i & 3]
    decryptedHeader[i*4..i*4+3] = (block ^ key) as bytes
```

**복호화된 32바이트 레이아웃:**
```
Offset  Size  Type  설명
0         4   RL   File header size (0x70)
4         4   RL   미사용
8         4   RL   CRC-32 (이 필드 0으로 마스킹 후 계산)
12        4   RL   미사용
16        4   RL   Section Page Map ID
20        8   RD   미사용
28        4   RL   Section Map ID
```

### R2007 Page Map 구조

```
// 파일 오프셋: FileHeader.pageMapOffset
// Page Map도 LZ77 압축 + checksum 포함

// LZ77 해제 후 내용:
pageCount entries, 각 entry:
  pageId  (RL, 4 bytes, signed)   -- 양수=data page, 음수=gap
  size    (RL, 4 bytes)            -- 페이지 크기
  // 파일 오프셋은 순차 누적 계산
```

### R2007 Section Map 구조

```
// LZ77 해제 후 내용 (R2004와 유사하나 name 인코딩이 UTF-16LE):
numSections (RL, 4 bytes)
반복:
  dataSize        (RL, 8 bytes, int64)
  maxDecompSize   (RL, 8 bytes, int64)
  unknown         (RL, 4 bytes)
  compressionType (RL, 4 bytes)
  sectionType     (RL, 4 bytes)
  pageCount       (RL, 4 bytes)
  padding         (2 bytes)
  nameLen         (RL, 4 bytes)   -- UTF-16LE 문자 수
  name            (UTF-16LE bytes, nameLen × 2)
  반복 pageCount:
    pageId        (RL, 4 bytes)
    dataSize      (RL, 4 bytes)
    offset        (RL, 8 bytes)
```

### R2007 섹션 페이지 체크섬 구조

각 데이터 페이지 앞에 0x20 (32 bytes) 페이지 헤더:
```
0        4   RL   Page type (섹션 타입)
4        4   RL   Decompressed size (이 페이지의 압축 해제 크기)
8        4   RL   Compressed size (이 페이지의 압축 크기)
12       4   RL   CRC-32 (페이지 데이터)
16      16   RC   미사용 또는 reserved
// 이후 compressed data (Compressed size bytes)
```

---

## 버전별 파일 구조 비교

| 항목 | R13/R14 | R2004 | R2007/R2010 | R2013/R2018 |
|---|---|---|---|---|
| 헤더 크기 | 가변 | 0x80 (128B) | 0x70+0x20 | 0x70+0x20 |
| 헤더 암호화 | 없음 | XOR(0x4848454C) | XOR(key schedule) | XOR(key schedule) |
| 섹션 구조 | 고정 오프셋 | Page+Section Map | Page+Section Map | Page+Section Map |
| 데이터 압축 | 없음 | LZ77 (선택) | LZ77 (필수) | LZ77 (필수) |
| 텍스트 인코딩 | CodePage | CodePage | UTF-16LE | UTF-16LE |
| CRC 방식 | CRC-16 | CRC-32 | CRC-32 | CRC-32 |
| Handle 구조 | Section 내 상대 | Section 내 상대 | Section 내 상대 | Section 내 상대 |

---

## 파일 읽기 전체 흐름 (모든 버전 공통)

```
1. 파일 첫 6바이트 읽기 → DwgVersionDetector.detect()
2. DwgFileStructureHandlerFactory.forVersion(version) → handler 획득
3. handler.readHeader(input) → FileHeaderFields
4. handler.readSections(input, header) → Map<String, SectionInputStream>
5. 각 SectionInputStream을 SectionParserRegistry 통해 파싱
```
