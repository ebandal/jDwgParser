# dwg-sections — 클래스 명세

> 섹션 파서/라이터. 스펙 §9(Header)~§28(XData) 구현.  
> 책임: `SectionInputStream`에서 도메인 객체를 생성하고, 반대로 도메인 객체를 `SectionOutputStream`에 직렬화하는 것.

---

## 패키지: `io.dwg.sections`

---

### `SectionParser<T>` *(interface)*

모든 섹션 파서의 계약.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `T parse(SectionInputStream stream, DwgVersion version)` | 스트림에서 도메인 객체 T를 파싱 |
| `String sectionName()` | 담당 섹션 이름 반환 (`SectionType` 상수 이용) |
| `boolean supports(DwgVersion version)` | 이 파서가 해당 버전을 지원하는지 |

---

### `SectionWriter<T>` *(interface)*

모든 섹션 라이터의 계약.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SectionOutputStream write(T data, DwgVersion version)` | 도메인 객체를 직렬화하여 SectionOutputStream 반환 |
| `String sectionName()` | 담당 섹션 이름 반환 |

---

### `AbstractSectionParser<T>` *(abstract)*

공통 편의 메서드 제공 기반 클래스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `protected BitStreamReader reader(SectionInputStream s, DwgVersion v)` | 스트림에서 BitStreamReader 생성 |
| `protected void skipBytes(BitStreamReader r, int count)` | count 바이트 skip (RC × count) |
| `protected void logUnknown(String field, long bitPos)` | unknown 필드 경고 로깅 |

---

### `SectionParserRegistry`

섹션 이름 → 파서 인스턴스 매핑 레지스트리.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void register(SectionParser<?> parser)` | 파서 등록 |
| `SectionParser<?> get(String sectionName)` | 이름으로 파서 조회. 미등록 시 UnknownSectionException |
| `Optional<SectionParser<?>> find(String sectionName)` | null-safe 조회 |
| `static SectionParserRegistry defaultRegistry()` | 기본 파서 전체 등록된 레지스트리 반환 |

---

## 패키지: `io.dwg.sections.header`

스펙 §9 (AcDb:Header — HEADER VARIABLES) 구현.

---

### `HeaderSectionParser`

헤더 변수 500+개를 파싱. 버전별로 존재 여부가 다른 변수는 DwgVersion 분기 처리.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `HeaderVariables parse(SectionInputStream stream, DwgVersion version)` | Sentinel 검증 → 크기(RL) 읽기 → 각 변수 순서대로 파싱 → 종료 Sentinel 검증 |
| `private void readCommonVariables(BitStreamReader r, HeaderVariables vars, DwgVersion v)` | 버전 무관 공통 변수 파싱 (DIMSCALE, LTSCALE 등) |
| `private void readVersionSpecificVariables(BitStreamReader r, HeaderVariables vars, DwgVersion v)` | 버전별 추가 변수 파싱 |
| `String sectionName()` | `"AcDb:Header"` 반환 |

---

### `HeaderSectionWriter`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SectionOutputStream write(HeaderVariables vars, DwgVersion version)` | 시작 Sentinel → 크기 자리 확보 → 변수 직렬화 → 크기 역기록 → 종료 Sentinel → CRC |

---

### `HeaderVariables`

스펙 §9의 모든 헤더 변수를 담는 컨테이너. 변수 500+개를 타입별 Map 또는 개별 필드로 저장.

**주요 필드 (일부)**
- `DwgHandleRef currentLayer` — 현재 레이어
- `String currentLineType`
- `double dimscale` — 치수 스케일
- `double ltscale` — 선종류 스케일
- `Point3D insBase` — 삽입 기준점
- `Point3D extMin`, `Point3D extMax` — 도면 범위
- `Point2D limMin`, `Point2D limMax` — 도면 한계
- `int lunits`, `int luprec` — 길이 단위/정밀도
- `boolean attmode`, `boolean blipmode`
- `DwgHandleRef dimstyle` — 치수 스타일 핸들

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `<T> T get(String varName)` | 이름으로 헤더 변수 조회 (타입 캐스팅) |
| `void set(String varName, Object value)` | 헤더 변수 값 설정 |
| `Map<String, Object> toMap()` | 전체 변수를 Map으로 반환 |

---

### `HeaderVariableReader`

개별 변수 이름과 타입에 따라 올바른 BitStreamReader 메서드를 호출하는 내부 헬퍼.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Object readVariable(BitStreamReader r, String name, DwgVersion v)` | 변수명으로 타입 조회 후 적절한 읽기 메서드 호출 |
| `static Map<String, Class<?>> VARIABLE_TYPES` | 변수명 → Java 타입 정적 맵 (빌드 시 초기화) |

---

## 패키지: `io.dwg.sections.classes`

스펙 §10 (AcDb:Classes) 구현.

---

### `ClassesSectionParser`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `List<DwgClassDefinition> parse(SectionInputStream stream, DwgVersion version)` | 시작 Sentinel → 크기(RL) 읽기 → 클래스 수 추정 반복 파싱 (크기로 종료 감지) → 종료 Sentinel |
| `private DwgClassDefinition parseOneClass(BitStreamReader r, DwgVersion v)` | classNum(BS)+version(BS)+appName(TV)+cppName(TV)+dxfName(TV)+wasZombie(B)+isEntity(BS) |
| `String sectionName()` | `"AcDb:Classes"` |

---

### `ClassesSectionWriter`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SectionOutputStream write(List<DwgClassDefinition> classes, DwgVersion version)` | 역순 직렬화. 크기는 후처리로 역기록 |

---

### `DwgClassDefinition`

커스텀 클래스 정의. 프록시 엔티티 처리에 필요.

**필드**
- `int classNumber` — 500 이상의 커스텀 객체 타입 번호
- `int version`
- `String applicationName`
- `String cppClassName`
- `String dxfRecordName`
- `boolean wasAZombie`
- `boolean isAnEntity`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `boolean isEntity()` | isAnEntity 반환 |
| `int classNumber()` | 타입 번호 반환 (ObjectTypeResolver에서 사용) |

---

### `DwgClassRegistry`

커스텀 클래스 번호 → DwgClassDefinition 매핑. ObjectTypeResolver에서 참조.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void register(DwgClassDefinition def)` | 등록 |
| `Optional<DwgClassDefinition> find(int classNumber)` | 번호로 조회 |
| `Optional<DwgClassDefinition> findByDxfName(String dxfName)` | DXF 이름으로 조회 |

---

## 패키지: `io.dwg.sections.handles`

스펙 §23 (AcDb:Handles — OBJECT MAP) 구현.

---

### `HandlesSectionParser`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `HandleRegistry parse(SectionInputStream stream, DwgVersion version)` | 섹션을 섹션 크기(RS) 단위로 읽음 → 각 레코드에서 handle offset(MC)+location offset(MC) 누적 합산 → HandleRegistry 구성 |
| `private void parseBlock(BitStreamReader r, HandleRegistry reg, long[] state)` | 하나의 핸들 맵 블록 파싱. state[0]=누적 핸들, state[1]=누적 오프셋 |
| `String sectionName()` | `"AcDb:Handles"` |

---

### `HandlesSectionWriter`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SectionOutputStream write(HandleRegistry registry, DwgVersion version)` | 핸들을 오름차순 정렬 후 delta(MC)+offset delta(MC) 쌍 직렬화. 블록 크기(RS) + CRC 포함 |

---

### `HandleRegistry`

핸들 값 → 파일 오프셋 매핑. 모든 객체 조회의 중심.

**필드**
- `Map<Long, Long> handleToOffset` — 핸들 → 파일 내 바이트 오프셋

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void put(long handle, long offset)` | 엔트리 추가 |
| `Optional<Long> offsetFor(long handle)` | 핸들로 오프셋 조회 |
| `int size()` | 총 핸들 수 |
| `Set<Long> allHandles()` | 전체 핸들 집합 반환 |
| `List<HandleEntry> sortedEntries()` | 핸들 오름차순 정렬된 엔트리 목록 |

---

### `HandleEntry` *(record)*

핸들과 오프셋의 쌍.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `HandleEntry(long handle, long offset)` | record 생성자 |

---

### `ObjectOffsetMap`

HandleRegistry의 래퍼. 타입별 핸들 그룹화 및 범위 조회 기능 추가.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `ObjectOffsetMap(HandleRegistry registry)` | 생성 |
| `Long offsetFor(long handle)` | 오프셋 반환. 없으면 null |
| `List<Long> handlesInRange(long startHandle, long endHandle)` | 범위 내 핸들 목록 |

---

## 패키지: `io.dwg.sections.objects`

스펙 §20 (AcDb:AcDbObjects) 구현. 모든 DWG 객체(엔티티+비엔티티)를 파싱.

---

### `ObjectsSectionParser`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `Map<Long, DwgObject> parse(SectionInputStream stream, DwgVersion version, HandleRegistry handles, DwgClassRegistry classes)` | 핸들 목록 순회 → 각 오프셋으로 seek → parseObject() 호출 → handle→object 맵 반환 |
| `private DwgObject parseObject(BitStreamReader r, DwgVersion v, DwgClassRegistry cls)` | 객체 크기(MS) → 타입(BS/OT) → ObjectTypeResolver로 파서 선택 → 공통 헤더 → 타입별 파싱 |
| `private CommonEntityData parseCommonEntityHeader(BitStreamReader r, DwgVersion v)` | §20 공통 엔티티 헤더: mode(BB)+numReactors(BL)+isXDic+inPaperSpace+layer+ltype+ltscale+color 등 |
| `String sectionName()` | `"AcDb:AcDbObjects"` |

---

### `ObjectsSectionWriter`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SectionOutputStream write(Map<Long, DwgObject> objects, DwgVersion version)` | 객체 순회 → 각 객체 직렬화 → 오프셋 맵 재계산 |
| `private byte[] writeObject(DwgObject obj, DwgVersion v)` | 공통 헤더 + 타입별 직렬화 |

---

### `ObjectReader` *(interface)*

개별 객체 타입의 추가 데이터 파싱 계약.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void read(DwgObject target, BitStreamReader r, DwgVersion v)` | target 객체에 타입 고유 필드 채우기 |
| `int objectType()` | 담당 객체 타입 번호 |

---

### `ObjectWriter` *(interface)*

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void write(DwgObject source, BitStreamWriter w, DwgVersion v)` | source 객체의 타입 고유 필드 직렬화 |
| `int objectType()` | 담당 객체 타입 번호 |

---

### `ObjectTypeResolver`

객체 타입 번호 → `ObjectReader` 인스턴스 매핑.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void register(ObjectReader reader)` | 표준 타입 파서 등록 |
| `void registerCustom(int classNum, ObjectReader reader)` | 커스텀 타입(500+) 파서 등록 |
| `Optional<ObjectReader> resolve(int typeCode)` | 타입 코드로 파서 조회 |
| `static ObjectTypeResolver defaultResolver(DwgClassRegistry classReg)` | 기본 타입 파서 전부 등록된 resolver 반환 |

---

## 패키지: `io.dwg.sections.aux`

스펙 §13~§22 보조 섹션 파서들.

---

### `SummaryInfoParser`

스펙 §13 (AcDb:SummaryInfo) 구현. 파일 메타데이터 (작성자, 제목 등).

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SummaryInfo parse(SectionInputStream stream, DwgVersion version)` | title(TV)+subject(TV)+author(TV)+keywords(TV)+comments(TV)+lastSavedBy(TV)+revisionNumber(TV)+totalEditingTime(RL)+createDate(RL)+updateDate(RL)+customProperties(키-값 쌍) 파싱 |
| `SectionOutputStream write(SummaryInfo info, DwgVersion version)` | 역순 직렬화 |

---

### `PreviewSectionParser`

스펙 §14 (AcDb:Preview). 썸네일 이미지 데이터.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `PreviewImage parse(SectionInputStream stream, DwgVersion version)` | 이미지 수(RL) → 각 이미지 타입(RC)+크기(RL)+데이터(RC×size) 파싱 |
| `SectionOutputStream write(PreviewImage preview, DwgVersion version)` | 직렬화 |

---

### `AppInfoParser`

스펙 §16 (AcDb:AppInfo). 작성 애플리케이션 정보.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `AppInfo parse(SectionInputStream stream, DwgVersion version)` | 버전 번호(BL)+XML 형식 앱 정보 문자열 파싱 |

---

### `FileDepListParser`

스펙 §17 (AcDb:FileDepList). 외부 참조 파일 목록.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `List<FileDepEntry> parse(SectionInputStream stream, DwgVersion version)` | 피처 카운트(BL) → 각 항목: filename(TV)+foundPath(TV)+fingerprint(TV)+version(TV)+isLoaded(B)+refCount(BL) |

---

### `AuxHeaderParser`

스펙 §27 (AcDb:AuxHeader). 보조 헤더 (R13+).

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `AuxHeader parse(SectionInputStream stream, DwgVersion version)` | RC×2(sentinel) + 각종 날짜/버전 필드 파싱 |

---

### `RevHistoryParser`

스펙 §18. 수정 이력.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `RevHistory parse(SectionInputStream stream, DwgVersion version)` | 이력 카운트(BL) → 각 항목 파싱 |

---

### `SecuritySectionParser`

스펙 §19. 암호화 파라미터.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `SecurityInfo parse(SectionInputStream stream, DwgVersion version)` | securityFlags(BL)+unknown+passwordLength+password 파싱 |

---

### `ObjFreeSpaceParser`

스펙 §21. 오브젝트 섹션 여유 공간 정보.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `ObjFreeSpace parse(SectionInputStream stream, DwgVersion version)` | numObjects(RL)+maxHandle(BLL)+lastOffset(RL) 파싱 |

---

### `VbaProjectParser`

스펙 §15 (AcDb:VBAProject). VBA 매크로 바이너리 블롭.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `byte[] parse(SectionInputStream stream, DwgVersion version)` | 전체 섹션 원시 바이트 반환 (파싱 없음) |

---

### `DataStorageParser`

스펙 §24 (AcDb:AcDsPrototype_1b). DataStorage 섹션.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DataStorage parse(SectionInputStream stream, DwgVersion version)` | 스키마 목록 + 데이터 레코드 파싱 |

---

### `TemplateSectionParser`

스펙 §22 (AcDb:Template). 도면 템플릿 측정 단위.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `TemplateInfo parse(SectionInputStream stream, DwgVersion version)` | 측정 단위(BS) 파싱 |

---

## 패키지: `io.dwg.sections.xdata`

스펙 §28 (Extended Entity Data) 구현.

---

### `XDataParser`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `List<XDataRecord> parse(BitStreamReader r, DwgVersion v)` | appName 핸들 → 반복 readXDataItem() → 다음 appName 또는 끝까지 |
| `private XDataRecord readXDataItem(BitStreamReader r, DwgVersion v)` | 그룹코드(RC) 읽기 → 코드에 따라 String/Short/Long/Double/Handle 등 읽기 |

---

### `XDataWriter`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void write(List<XDataRecord> records, BitStreamWriter w, DwgVersion v)` | 각 레코드의 그룹코드(RC) + 값 직렬화 |

---

### `XDataRecord`

XData 항목 하나. 그룹코드와 값의 쌍.

**필드**
- `int groupCode` — DWG XData 그룹코드
- `Object value` — String / Integer / Long / Double / long[](handle) 등

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `XDataRecord(int groupCode, Object value)` | 생성자 |
| `int groupCode()` | 그룹코드 반환 |
| `<T> T valueAs(Class<T> type)` | 값 타입 캐스팅 반환 |
| `boolean isString()` | groupCode 1000-1009 범위 여부 |

---

### `ProxyEntityParser`

스펙 §29 (PROXY ENTITY GRAPHICS). 프록시 엔티티의 그래픽 데이터.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `ProxyGraphics parse(BitStreamReader r, DwgVersion v)` | graphicsDataSize(BL) → 그래픽 바이트 읽기 → entityDataSize(BL) → 엔티티 바이트 읽기 |
| `static boolean hasProxyGraphics(DwgObject obj)` | obj의 공통 헤더 flag 확인 |

---

---

# 상세 설계 보충

---

## 🔷 버전별 섹션 구조 (R13~R2018)

이 섹션은 모든 DWG 버전에서 Handles/Objects 섹션이 어떻게 다르게 처리되는지를 보여준다.

### 버전 그룹분류

| 그룹 | 버전 | 압축 | 암호화 | 섹션레이아웃 | 문자열 |
|-----|------|------|-------|----------|-------|
| **A** | R13, R13c3, R14, R2000, R2000i, R2002 | ❌ | ❌ | 고정(헤더지정) | TV (ANSI) |
| **B** | R2004, R2004a~c | ✅ LZ77 | ❌ | 동적(섹션정보표) | TV (ANSI) |
| **C** | R2007~R2009 | ✅ LZ77 | ✅ 페이지헤더 | 동적(확장) | TU (Unicode) |
| **D** | R2010, R2010b, R2012 | ✅ LZ77 | ✅ 페이지헤더 | 동적(Class확장) | TU (Unicode) |
| **E** | R2013~R2017 | ✅ LZ77 | ✅ 페이지헤더 | 동적(추가확장) | TU (Unicode) |
| **F** | R2018+ | ✅ LZ77 | ✅ 페이지헤더 | 동적(헤더확장) | TU (Unicode) |

### 그룹 A (R13~R2002) - 고정 레이아웃, 압축 없음

**Handles Section (§23) 구조:**

```
여러 페이지로 구성:

[RS_BE]            page_size         (2 bytes, big-endian, 보통 2032~2040)
[page_size - 4 bytes]                handle-offset 쌍들의 델타 인코딩 데이터
[RS_BE]            crc               (2 bytes, big-endian, seed=0xC0C1)

마지막 페이지: page_size == 2 (데이터 없음, 종료 신호)
```

**Objects Section (§20) 구조:**

```
Offset 0부터 시작 (헤더 없음):
  [MS] objectSize
  [BS] typeCode
  [Handle] objectHandle
  [BS] xDataSize
  [XData or empty] xData
  ... 공통 헤더 ...
  ... 타입별 데이터 ...
  
다음 객체는 이전 객체 끝에서 계속됨
Handles section의 offset은 Objects 내 절대 바이트 위치
```

**⚠️ 중요 세부사항:**
- `page_size`는 **RS_BE (big-endian)**로 읽음 (가장 중요한 바이트가 먼저)
- `handle_delta` = UMC (unsigned, 항상 양수)
- `offset_delta` = MC (signed, 음수 가능 - 누적됨)
- Objects 섹션은 offset 0부터 시작 (240바이트 헤더 없음)

### 그룹 B (R2004) - LZ77 압축, 동적 섹션배치

**파일 구조:**

```
[Header Section]  ← 암호화 안 됨
├── Header Variables
└── Section Info Table (동적 섹션 매핑)

[Compressed Section Pages]
├── Page N (압축됨)
├── Page N+1 (압축됨)
└── System Section Page (섹션 맵, 압축 가능)
```

**Handles Section (R2004):**

```
1. 섹션 정보표에서 HANDLES section 위치 → 페이지 목록 읽기
2. 각 페이지 (32바이트 헤더 + 압축데이터) → LZ77 해제
3. 결과 바이트스트림에서 Handles 파싱:

   [RS_BE]          page_size
   [1-3 bytes]      handle_delta (UMC)
   [1-3 bytes]      offset_delta (MC, signed)
   ... 반복 ...
   [RS_BE]          crc
```

**Objects Section (R2004):**

```
1. 섹션 정보표에서 OBJECTS section 위치 → 페이지 목록 읽기
2. 각 페이지 → LZ77 해제
3. 결과 바이트스트림에서 Objects 파싱:
   - Offset 0부터 시작 (헤더 없음)
   - Handles의 offset으로 객체 위치 결정
```

**페이지 헤더 (32 bytes, 암호화 안 됨):**

```
[u32] page_type               (0x4163043b expected)
[u32] section_type            (0 = OBJECTS, other values for other sections)
[u32] data_size_compressed    
[u32] page_size
[u32] start_offset
[u32] unknown
[u32] page_header_crc
[u32] data_crc
```

**LZ77 opcode 구조:**

```
While reading:
  [B] opcode
  
  If opcode < 0x10:                    // Literal (1-16 bytes)
    copy_count = opcode + 1
    copy literal_bytes to output
  
  Else if opcode < 0x20:               // Medium match
    offset_bytes = 2 bytes
    length_bits = opcode - 0x10
    offset = offset_bytes | ((length_bits >> 2) << 8)
    length = (length_bits & 0x03) + 4
    copy (output_pos - offset) to output, length bytes
  
  Else if opcode < 0x100:              // Long match
    offset_bytes = 2 bytes
    length_byte = 1 byte
    offset = offset_bytes | ((opcode >> 2) << 8)
    length = length_byte + (((opcode & 0x03) << 8) + 0x100)
    copy (output_pos - offset) to output, length bytes
  
  If opcode == 0x11:
    break  // Decompression complete
```

### 그룹 C (R2007~R2009) - 페이지헤더 암호화, 문자열 Unicode

**주요 변경사항:**

1. **Page Header Encryption (32 bytes):**
   ```
   secMask = 0x4164536b ^ (page_address & 0xFFFFFFFF)
   
   For each 4-byte word in header:
     word ^= (secMask >> ((offset_in_page % 4) * 8)) & 0xFF
   ```

2. **String Encoding 변경:**
   - R2004까지: TV (ANSI code page based)
   - R2007부터: TU (UTF-16LE, Unicode)

3. **Section Info Table 확장:**
   - 추가 필드들 (bitsize, compression flags 등)
   - Section 매핑 더 복잡함

4. **Handles/Objects 섹션 포맷:**
   - LZ77 압축 (R2004와 동일)
   - 페이지 헤더만 암호화됨
   - 데이터 자체는 암호화 안 됨

### 그룹 D-F (R2010+) - 점진적 확장

| 버전 | 추가사항 |
|-----|---------|
| R2010+ | Class definition에 hsize 필드 추가 |
| R2013+ | 추가 metadata 통합 |
| R2018+ | Header 자체 hsize 필드 추가 |

**⚠️ 핵심:** Handles/Objects 섹션 포맷은 R2004~R2018 동안 변경 없음. 변경은 주로 Header 스키마에만 영향.

---

## 섹션 공통 Sentinel 패턴

R13/R14의 섹션(Header, Classes, Object Map)은 모두 동일한 구조를 따른다:

```
[16 bytes]  Section Start Sentinel
[4 bytes]   RL  Data size (아래 데이터 크기, Sentinel·CRC 제외)
[N bytes]   Section data (BitStreamReader로 읽는 영역)
[2 bytes]   RS  CRC-16 (seed=0xC0C1, 헤더+데이터 전체 대상)
[16 bytes]  Section End Sentinel  (Start Sentinel 각 바이트 비트 반전)
```

R2004+는 이 Sentinel 구조를 사용하지 않는다. 대신 Section Map의 `dataSize` 필드로 경계를 확정한다.

---

## Header Section 파싱 상세 (§9)

### 헤더 변수 파싱 순서

R2000+ 기준 주요 변수의 파싱 순서 (BitStreamReader 기준):

```
Sentinel (16 bytes)
Size (RL)         ← 아래 변수 전체 크기
CRC seed (RL)     ← R13: 없음, R2000+: 존재

// 주요 변수 목록 (순서 고정, 버전별 일부 생략)
ACADMAINTVER     BS
ACADVER          TV
DWGCODEPAGE      TV
INSBASE          3BD
EXTMIN           3BD
EXTMAX           3BD
LIMMIN           2RD
LIMMAX           2RD
ORTHOMODE        BS
REGENMODE        BS
FILLMODE         BS
QTEXTMODE        BS
PSLTSCALE        BS
ATTMODE          BS
INSUNITS         BS
AUNITS           BS
AUPREC           BS
USERI1..5        BS (5개)
USERR1..5        BD (5개)
LTSCALE          BD
TEXTSIZE         BD
TRACEWID         BD
SKETCHINC        BD
FILLETRAD        BD
THICKNESS        BD
ANGBASE          BD
PDSIZE           BD
PLINEWID         BD
DIMSCALE         BD
DIMASZ           BD
DIMEXO           BD
DIMDLI           BD
DIMEXE           BD
DIMRND           BD
DIMDLE           BD
DIMTP            BD
DIMTM            BD
DIMTXT           BD
DIMCEN           BD
DIMTSZ           BD
DIMALTF          BD
DIMLFAC          BD
DIMTVP           BD
DIMTFAC          BD
DIMGAP           BD
DIMTOL           B
DIMLIM           B
DIMTIH           B
DIMTOH           B
DIMSE1           B
DIMSE2           B
DIMALT           B
DIMTOFL          B
DIMSAH           B
DIMTIX           B
DIMSOXD          B
...
(500여 개, 스펙 §9 순서 준수)
...

CRC (RS, 2 bytes)
Sentinel End (16 bytes)
```

### HeaderVariables 타입 맵 초기화 전략

`HeaderVariableReader.VARIABLE_TYPES` 빌드:
- 스펙 §9의 변수 목록을 정적 초기화 블록에서 `Map.of(...)` 또는 `HashMap`으로 구성
- key = 변수 이름 문자열 (예: `"DIMSCALE"`)
- value = `Class<?>` (예: `Double.class`, `Integer.class`, `Point3D.class`, `DwgHandleRef.class`)

---

## Handles Section 파싱 상세 (§23)

⚠️ **중요:** libredwg 검증 결과, 모든 DWG 버전(R13~R2018)에서 동일한 구조 사용

### Object Map 인코딩 구조 (모든 버전)

Object Map은 여러 페이지로 구성. 각 페이지:

```
[2 bytes RS_BE]  page_size         (이 페이지의 바이트 크기, CRC 포함. BIG-ENDIAN!)
[ page_size - 4 bytes ]            handle-offset 쌍들의 델타 인코딩 데이터
[2 bytes RS_BE]  CRC-16            (seed=0xC0C1, BIG-ENDIAN!)
```

**주요 수정사항:**
- `RS` → `RS_BE` (BIG-ENDIAN, 가장 중요한 바이트 먼저)
- 페이지 크기는 보통 2032~2040 바이트
- 마지막 페이지: `page_size == 2` (데이터 없음 → 종료 신호)

### Delta 인코딩 알고리즘 (모든 버전 동일)

```
last_handle = 0
last_offset = 0

while hasMorePages:
    page_size = readBigEndianShort()  // RS_BE - 매우 중요!
    if page_size <= 2:
        break  // 종료 신호
    
    page_start = current_position
    while (current_position - page_start) < (page_size - 4):
        handle_delta = readUnsignedModularChar()  // UMC - 항상 양수
        offset_delta = readSignedModularChar()    // MC - 음수 가능! (오프셋이 역방향일 수 있음)

        current_handle = last_handle + handle_delta
        current_offset = last_offset + offset_delta

        registry.put(current_handle, current_offset)

        last_handle = current_handle
        last_offset = current_offset
    
    crc = readBigEndianShort()  // RS_BE
    validateCRC(crc, page_start, page_size)
```

**R13/R14와의 차이점:**
- RS_BE 사용 (R13/R14는 RS 사용)
- handle_delta는 UMC (unsigned - 항상 양수)
- offset_delta는 MC (signed - 음수 가능)
- 각 페이지는 독립적인 CRC 검증

### HandlesSectionWriter Delta 인코딩

```
entries = registry.sortedEntries()  // handle 오름차순 정렬

last_handle = 0
last_offset = 0
current_block_bytes = ByteArrayOutputStream

for entry in entries:
    delta_h = entry.handle - last_handle
    delta_o = entry.offset - last_offset

    encoded_h = ModularChar.encode(delta_h)
    encoded_o = ModularChar.encode(delta_o)

    if (current_block_bytes.size() + encoded_h.length + encoded_o.length + 4) > MAX_BLOCK_SIZE:
        // 현재 블록 flush (size RS + data + CRC RS)
        flushBlock(current_block_bytes, output)
        current_block_bytes.reset()

    current_block_bytes.write(encoded_h)
    current_block_bytes.write(encoded_o)

    last_handle = entry.handle
    last_offset = entry.offset

// 마지막 블록 + 종료 블록 (size=2) flush
```

---

## Objects Section 파싱 파이프라인 (§20)

⚠️ **중요:** 모든 DWG 버전(R13~R2018)에서 Objects section은 **헤더 없이** 바이트 오프셋 0부터 객체 데이터가 시작됨 (libredwg 검증)
- R13~R2002: 직접 Objects 데이터 (비트스트림)
- R2004~R2018: LZ77 해제 후 동일한 구조 사용

### 전체 파이프라인

```
ObjectsSectionParser.parse(stream, version, handles, classes):

1. SectionInputStream → BitStreamReader 생성
2. HandleRegistry의 모든 handle 순회:
   a. offsetFor(handle) → byteOffset (누적 오프셋)
   b. BitStreamReader.seek(byteOffset * 8)  // 바이트 오프셋 → 비트 오프셋 변환
   c. parseOneObject(reader, version, classes) 호출
3. 결과: Map<Long handle, DwgObject> 반환

주의: Objects section은 offset 0부터 시작. "240바이트 헤더"는 존재하지 않음.
      Handles 섹션의 offset은 Objects 섹션 내 절대 바이트 위치.
```

### Offset의 누적 계산 (모든 버전)

```
// Handles section에서 읽은 deltas는 누적됨
cumulative_offset = 0
last_handle = 0
last_offset = 0

for each (handle_delta, offset_delta) in handles:
    cumulative_offset += offset_delta  // offset_delta는 signed (음수 가능)
    
    byte_position = cumulative_offset  // Objects section 내 절대 위치
    
    // Objects section의 byte_position에서 객체 파싱
    DwgObject obj = parseObjectAt(objects_data, byte_position)
    registry.put(last_handle + handle_delta, obj)
```

### 단일 객체 파싱 순서

```
parseOneObject(reader, version, classes):

// 1. 객체 크기 (MS 타입)
objectSize = reader.readModularShort()  // 전체 객체 비트 크기

// 2. 객체 타입 (BS 또는 타입 코드)
typeCode = reader.readBitShort()
// typeCode >= 500: 커스텀 클래스 (DwgClassRegistry 참조)

// 3. 공통 객체 헤더 (모든 객체 공통)
handle     = reader.readHandle()      // 이 객체 자신의 핸들
xDataSize  = reader.readBitShort()    // XData 크기 (0이면 없음)
if xDataSize > 0:
    xData  = XDataParser.parse(reader, version)

numReactors = reader.readBitLong()
isXDicMissing = reader.readBit()      // R2004+
if !isXDicMissing:
    xDicHandle = reader.readHandle()

ownerHandle = reader.readHandle()

// 4. 엔티티인 경우: 공통 엔티티 헤더 추가 파싱
if DwgObjectType.fromCode(typeCode).isEntity():
    parseCommonEntityHeader(reader, version) → CommonEntityData

// 5. 타입별 파싱
ObjectReader objectReader = typeResolver.resolve(typeCode)
if objectReader != null:
    objectReader.read(targetObject, reader, version)
else:
    // UnknownEntityPolicy에 따라 skip/throw/wrap
```

### CommonEntityData 파싱 순서 (§20 공통 엔티티 헤더)

**R13/R14 vs R2000+의 차이점을 명시적으로 처리할 것:**

```
entityMode    = reader.readBits(2)    // BB
numReactors   = reader.readBitLong()  // BL
noLinks       = reader.readBit()      // B (R2000+만 읽음, R13/R14는 없음)

if version < R2004:
    colorIndex = reader.readBitShort()  // BS
else:
    colorIndex = reader.readBitShort()  // BS
    if (colorIndex & 0x2000) != 0:      // RGB 플래그
        rgb = reader.readBitLong()       // BL
    if (colorIndex & 0x4000) != 0:      // 색상명/책 플래그
        colorName = reader.readVariableText()  // TV
        bookName  = reader.readVariableText()  // TV

linetypeFlags = reader.readBits(2)    // 2B
plotstyleFlags = reader.readBits(2)   // 2B (R2000+)

if version >= R2007:
    materialFlags = reader.readBits(2)  // 2B
    shadowFlags   = reader.readRawChar() // RC

invisibility  = reader.readBitShort()  // BS
lineWeight    = reader.readRawChar()   // RC

// 핸들 참조 (CED 끝부분)
layerHandle    = reader.readHandle()
if linetypeFlags == 0x03:              // explicit linetype
    lineTypeHandle = reader.readHandle()
if version >= R2000 && plotstyleFlags == 0x03:
    plotStyleHandle = reader.readHandle()
if version >= R2007:
    if materialFlags == 0x03: materialHandle = reader.readHandle()
    if shadowFlags bits: visualStyleHandle = reader.readHandle()
```

---

## 주요 객체 타입 바이너리 레이아웃 (§20)

### LINE (type=0x13)

```
// R2000+에서만 존재하는 최적화 플래그
if version >= R2000:
    hasZ = reader.readBit()  // B: Z 좌표 포함 여부

// start point
start.x = reader.readRawDouble()  // RD
start.y = reader.readRawDouble()  // RD
if hasZ || version < R2000:
    start.z = reader.readRawDouble()  // RD
else:
    start.z = 0.0

// end point (R2000+에서 2BD, R13/R14에서 3RD)
if version >= R2000:
    end.x = reader.readBitDoubleWithDefault(start.x)  // DD
    end.y = reader.readBitDoubleWithDefault(start.y)  // DD
    if hasZ: end.z = reader.readBitDoubleWithDefault(start.z)
else:
    end.x = reader.readRawDouble()  // RD
    end.y = reader.readRawDouble()  // RD
    end.z = reader.readRawDouble()  // RD

thickness  = reader.readBitThickness()   // BT
extrusion  = reader.readBitExtrusion()   // BE (3BD or bit)

// 핸들 참조 (공통 엔티티 헤더 이후)
// (layerHandle, lineTypeHandle 등은 공통 헤더에서 읽음)
```

### CIRCLE (type=0x12)

```
center.x   = reader.readRawDouble()   // RD
center.y   = reader.readRawDouble()   // RD
center.z   = reader.readRawDouble()   // RD
radius     = reader.readBitDouble()   // BD
thickness  = reader.readBitThickness()// BT
extrusion  = reader.readBitExtrusion()// BE
```

### ARC (type=0x11)

```
center.x   = reader.readRawDouble()  // RD
center.y   = reader.readRawDouble()  // RD
center.z   = reader.readRawDouble()  // RD
radius     = reader.readBitDouble()  // BD
thickness  = reader.readBitThickness()
extrusion  = reader.readBitExtrusion()
startAngle = reader.readBitDouble()  // BD (radians)
endAngle   = reader.readBitDouble()  // BD (radians)
```

### LWPOLYLINE (type=0x4D)

```
flags         = reader.readBitShort()         // BS
if flags & 0x04: constantWidth = reader.readBitDouble()  // BD
if flags & 0x08: elevation     = reader.readBitDouble()  // BD
if flags & 0x02: thickness     = reader.readBitDouble()  // BD
if flags & 0x01: extrusion     = reader.read3BitDouble() // 3BD
numVertices   = reader.readBitLong()           // BL
numBulges     = reader.readBitLong()           // BL
numWidths     = reader.readBitLong()           // BL (R2000+)

for i in 0..numVertices-1:
    if i == 0:
        vertices[0].x = reader.readRawDouble() // RD
        vertices[0].y = reader.readRawDouble() // RD
    else:
        vertices[i].x = reader.readBitDoubleWithDefault(vertices[i-1].x) // DD
        vertices[i].y = reader.readBitDoubleWithDefault(vertices[i-1].y) // DD

for i in 0..numBulges-1:
    bulges[i] = reader.readBitDouble()         // BD

for i in 0..numWidths-1:
    widths[i][0] = reader.readBitDouble()      // BD start width
    widths[i][1] = reader.readBitDouble()      // BD end width
```

### TEXT (type=0x01)

```
if version <= R14:
    insertionHeight = reader.readBitDouble()   // BD

insertPoint.x = reader.readRawDouble()         // RD
insertPoint.y = reader.readRawDouble()         // RD

height        = reader.readBitDouble()         // BD
text          = reader.readVariableText()      // TV (T or TU)
rotation      = reader.readBitDouble()         // BD
widthFactor   = reader.readBitDouble()         // BD
oblique       = reader.readBitDouble()         // BD
generation    = reader.readRawChar()           // RC
horizAlign    = reader.readBitShort()          // BS
vertAlign     = reader.readBitShort()          // BS

alignmentPoint.x = reader.readRawDouble()      // RD
alignmentPoint.y = reader.readRawDouble()      // RD

extrusion     = reader.readBitExtrusion()      // BE
thickness     = reader.readBitThickness()      // BT

// 핸들 참조
styleHandle   = reader.readHandle()            // H
```

### INSERT (type=0x07)

```
insertPoint.x  = reader.readRawDouble()  // RD
insertPoint.y  = reader.readRawDouble()  // RD
insertPoint.z  = reader.readRawDouble()  // RD

if version <= R14:
    scale.x = reader.readBitDouble()     // BD
    scale.y = reader.readBitDouble()     // BD
    scale.z = reader.readBitDouble()     // BD
else:
    scalePresent = reader.readBit()      // B: 스케일이 (1,1,1)이 아니면 true
    if scalePresent:
        scale.x = reader.readBitDouble() // BD
        scale.y = reader.readBitDoubleWithDefault(scale.x)  // DD
        scale.z = reader.readBitDoubleWithDefault(scale.x)  // DD
    else:
        scale = {1.0, 1.0, 1.0}

rotation       = reader.readBitDouble()  // BD
extrusion      = reader.readBitExtrusion()
hasAttribs     = reader.readBit()        // B

// 핸들 참조
blockHandle    = reader.readHandle()     // H → BLOCK_HEADER
if hasAttribs:
    firstAttribHandle = reader.readHandle()
    lastAttribHandle  = reader.readHandle()
    seqEndHandle      = reader.readHandle()
```

---

## Classes Section 파싱 상세 (§10)

### 구조

```
Sentinel (16 bytes)
Size (RL)

반복 (Size 기반 종료 감지):
    classNum          BS   (500 이상, 커스텀 클래스 번호)
    version           BS
    applicationName   TV
    cppClassName      TV
    dxfRecordName     TV
    wasAZombie        B
    isAnEntity        BS   (0=object, 1=entity)

CRC-16 (RS)
Sentinel End (16 bytes)
```

### DwgClassRegistry와의 연계

`ObjectTypeResolver`는 `DwgClassRegistry`를 참조한다:
- 표준 타입 코드(0x01~0xFF): `ObjectTypeResolver`에 직접 등록된 `ObjectReader`
- 커스텀 타입 코드(500+): `DwgClassRegistry.find(classNum)` → DXF 이름으로 적절한 ObjectReader 매핑

---

## SummaryInfo 파싱 상세 (§13)

```
title       TV
subject     TV
author      TV
keywords    TV
comments    TV
lastSavedBy TV
revisionNum TV
editingTimeTotal  RL
createDate        RL  (Julian day number * 86400 + seconds 이후 정수)
updateDate        RL

// Custom properties (key-value 쌍)
numCustomProps    BS
반복 numCustomProps:
    key    TV
    value  TV
```
