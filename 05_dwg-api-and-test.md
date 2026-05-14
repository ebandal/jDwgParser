# dwg-api — 클래스 명세

> 라이브러리 사용자가 직접 접촉하는 퍼블릭 API.  
> 내부 모듈(core/format/sections/entities)을 조합하여 간결한 Fluent / Builder 인터페이스를 제공.

---

## 패키지: `io.dwg.api`

---

### `DwgReader`

DWG 파일 읽기 진입점. Fluent Builder 패턴.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static DwgReader withOptions(DwgReadConfig config)` | 설정을 가진 Reader 빌더 반환 |
| `static DwgReader defaultReader()` | 기본 설정으로 Reader 반환 |
| `DwgDocument open(Path filePath)` | 파일을 읽어 DwgDocument 반환. 내부 순서: ① 버전 감지 → ② 포맷 핸들러 선택 → ③ 섹션 추출 → ④ 각 섹션 파서 실행 → ⑤ 핸들 그래프 구성 → ⑥ DwgDocument 반환 |
| `DwgDocument open(byte[] data)` | 바이트 배열에서 직접 읽기 |
| `DwgDocument open(InputStream stream)` | 스트림에서 읽기 (메모리로 완전히 로드) |
| `DwgVersion detectVersion(Path filePath)` | 파일 열지 않고 버전만 빠르게 반환 |

---

### `DwgWriter`

DWG 파일 쓰기 진입점.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static DwgWriter forVersion(DwgVersion version)` | 지정 버전으로 쓰기 위한 Writer 반환 |
| `static DwgWriter withOptions(DwgWriteConfig config)` | 설정 기반 Writer 반환 |
| `void write(DwgDocument document, Path filePath)` | ① 섹션 직렬화 → ② 핸들 맵 재계산 → ③ 포맷 핸들러로 파일 구조 작성 → ④ 파일 저장 |
| `void write(DwgDocument document, OutputStream stream)` | 스트림으로 쓰기 |
| `byte[] toBytes(DwgDocument document)` | 바이트 배열로 반환 |

---

### `DwgDocument`

파싱된 DWG 파일 전체를 나타내는 루트 객체.

**필드**
- `DwgVersion version`
- `HeaderVariables headerVariables`
- `Map<Long, DwgObject> objectMap` — handle → DwgObject
- `HandleRegistry handleRegistry`
- `DwgClassRegistry classRegistry`
- `List<DwgClassDefinition> customClasses`
- `SummaryInfo summaryInfo`
- `PreviewImage preview`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgVersion version()` | 파일 버전 반환 |
| `HeaderVariables header()` | 헤더 변수 반환 |
| `List<DwgEntity> entities()` | 전체 엔티티 목록 (Model Space 기준) |
| `List<DwgEntity> entitiesInBlock(String blockName)` | 지정 블록 내 엔티티 목록 |
| `List<DwgLayerEntry> layers()` | 전체 레이어 목록 |
| `Optional<DwgLayerEntry> layer(String name)` | 이름으로 레이어 조회 |
| `List<DwgBlockHeader> blocks()` | 전체 블록 헤더 목록 |
| `Optional<DwgBlockHeader> block(String name)` | 이름으로 블록 조회 |
| `<T extends DwgObject> Optional<T> objectByHandle(long handle, Class<T> type)` | 핸들로 객체 조회 후 타입 캐스팅 |
| `<T extends DwgObject> List<T> objectsOfType(Class<T> type)` | 지정 타입의 모든 객체 목록 |
| `DwgQueryBuilder query()` | 쿼리 빌더 반환 |
| `SummaryInfo summaryInfo()` | 파일 메타 정보 반환 |
| `Optional<PreviewImage> preview()` | 썸네일 이미지 반환 |

---

### `DwgDocumentBuilder`

빈 DwgDocument를 프로그래밍 방식으로 구성.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static DwgDocumentBuilder create(DwgVersion version)` | 지정 버전의 빈 문서 빌더 반환 |
| `DwgDocumentBuilder addLayer(DwgLayerEntry layer)` | 레이어 추가. 핸들 자동 할당 |
| `DwgDocumentBuilder addEntity(DwgEntity entity)` | Model Space에 엔티티 추가 |
| `DwgDocumentBuilder addEntity(DwgEntity entity, String blockName)` | 지정 블록에 엔티티 추가 |
| `DwgDocumentBuilder addBlock(DwgBlockHeader block)` | 블록 추가 |
| `DwgDocumentBuilder setHeaderVariable(String name, Object value)` | 헤더 변수 설정 |
| `DwgDocument build()` | 핸들 그래프 완성 후 DwgDocument 반환 |

---

### `DwgQuery` *(interface)*

쿼리 조건 계약.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `List<DwgEntity> execute(DwgDocument document)` | 조건에 맞는 엔티티 목록 반환 |

---

### `DwgQueryBuilder`

메서드 체이닝으로 엔티티 필터 쿼리 구성.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgQueryBuilder onLayer(String layerName)` | 레이어 이름 필터 추가 |
| `DwgQueryBuilder onLayers(Collection<String> names)` | 복수 레이어 필터 |
| `DwgQueryBuilder ofType(DwgObjectType type)` | 객체 타입 필터 |
| `DwgQueryBuilder ofType(Class<? extends DwgEntity> clazz)` | Java 타입 필터 |
| `DwgQueryBuilder inBoundingBox(Point3D min, Point3D max)` | 바운딩박스 내 엔티티 필터 |
| `DwgQueryBuilder withColor(CmColor color)` | 색상 필터 |
| `DwgQueryBuilder inBlock(String blockName)` | 특정 블록 내 엔티티로 제한 |
| `DwgQueryBuilder withHandle(long handle)` | 특정 핸들 필터 |
| `List<DwgEntity> execute()` | 필터 적용 후 결과 반환 |
| `DwgQueryResult executeWithResult()` | 통계 포함 결과 반환 |
| `long count()` | execute() 후 카운트만 반환 |
| `Optional<DwgEntity> first()` | 첫 번째 결과 반환 |

---

### `DwgQueryResult`

쿼리 실행 결과 컨테이너.

**필드**
- `List<DwgEntity> entities`
- `long totalScanned`
- `Duration executionTime`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `List<DwgEntity> entities()` | 결과 엔티티 목록 |
| `int size()` | 결과 수 |
| `Map<DwgObjectType, Long> countByType()` | 타입별 카운트 집계 |
| `Map<String, Long> countByLayer()` | 레이어별 카운트 집계 |

---

## 패키지: `io.dwg.api.config`

---

### `DwgReadConfig`

읽기 동작 설정.

**필드**
- `UnknownEntityPolicy unknownEntityPolicy` — SKIP / THROW / WRAP_AS_PROXY
- `boolean strictCrcCheck` — CRC 실패 시 예외 여부 (기본 false)
- `boolean strictSentinelCheck` — Sentinel 실패 시 예외 여부 (기본 false)
- `boolean loadPreview` — 미리보기 섹션 로드 여부 (기본 false, 성능)
- `boolean lazyObjectResolution` — 핸들 참조를 지연 해석 여부 (기본 true)
- `Set<DwgVersion> supportedVersions` — 허용 버전 집합

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static DwgReadConfig defaults()` | 기본값으로 설정된 인스턴스 반환 |
| `static DwgReadConfig strict()` | 엄격 모드 (CRC+Sentinel 모두 검사) |
| `DwgReadConfig withUnknownPolicy(UnknownEntityPolicy p)` | 미지 엔티티 정책 변경 후 새 설정 반환 |

---

### `DwgWriteConfig`

쓰기 동작 설정.

**필드**
- `DwgVersion targetVersion` — 출력 버전
- `boolean updateTimestamps` — 저장 시 날짜 자동 갱신 여부
- `boolean recalculateHandles` — 핸들 재할당 여부
- `boolean writeThumbnail` — 썸네일 포함 여부

---

### `UnknownEntityPolicy` *(enum)*

| 상수 | 처리 내용 |
|---|---|
| `SKIP` | 알 수 없는 엔티티 무시하고 계속 진행 |
| `THROW` | DwgParseException 발생 |
| `WRAP_AS_PROXY` | DwgProxyEntity로 감싸서 보존 |

---

## 패키지: `io.dwg.api.event`

---

### `DwgParseListener` *(interface)*

파싱 진행 상황 콜백 인터페이스.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void onProgress(ParseProgressEvent event)` | 섹션 완료 등 진행 이벤트 수신 |
| `void onWarning(ParseWarningEvent event)` | 경고(unknown 필드 등) 수신 |
| `void onEntityParsed(EntityParsedEvent event)` | 엔티티 1개 파싱 완료 시 수신 |
| `void onComplete(DwgDocument document)` | 전체 파싱 완료 시 수신 |

---

### `ParseProgressEvent` *(record)*

**필드**
- `String currentSection` — 현재 처리 중인 섹션 이름
- `int completedSections`, `int totalSections`
- `double progressRatio` — 0.0 ~ 1.0

---

### `ParseWarningEvent` *(record)*

**필드**
- `String message`
- `long bitOffset`
- `String context` — 경고 발생 섹션/객체 컨텍스트

---

### `EntityParsedEvent` *(record)*

**필드**
- `long handle`
- `DwgObjectType type`
- `int totalParsed`

---

## 패키지: `io.dwg.api.convert`

---

### `DwgConverter` *(interface)*

DWG 문서 변환 계약.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgDocument convert(DwgDocument source, DwgWriteConfig targetConfig)` | 소스 버전 → 타겟 버전 변환 |

---

### `DwgVersionConverter`

버전 간 변환 로직. 버전 업그레이드/다운그레이드.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgDocument convert(DwgDocument source, DwgWriteConfig config)` | ① 소스 버전 확인 → ② 타겟 버전 호환성 검사 → ③ 버전 간 필드 매핑 → ④ 새 DwgDocument 반환 |
| `boolean canConvert(DwgVersion from, DwgVersion to)` | 변환 가능 여부 (다운그레이드 일부 불가) |
| `List<String> conversionWarnings(DwgVersion from, DwgVersion to)` | 데이터 손실 가능성 경고 목록 |

---

### `EntitySerializer` *(interface)*

특정 엔티티 타입의 직렬화 계약. `ObjectWriter`의 API 계층 래퍼.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void serialize(DwgEntity entity, BitStreamWriter writer, DwgVersion version)` | 엔티티 직렬화 |
| `Class<? extends DwgEntity> targetType()` | 담당 엔티티 클래스 |

---

### `EntityDeserializer` *(interface)*

특정 엔티티 타입의 역직렬화 계약. `ObjectReader`의 API 계층 래퍼.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `DwgEntity deserialize(BitStreamReader reader, DwgVersion version)` | 엔티티 역직렬화 |
| `DwgObjectType objectType()` | 담당 객체 타입 |

---

---

# dwg-test — 클래스 명세

> JUnit 5 기반 단위/통합 테스트, Fuzz 테스트, JMH 벤치마크.

---

## 패키지: `io.dwg.test.unit`

---

### `BitStreamReaderTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testReadBitShort_zero()` | opcode=10 → 0 반환 검증 |
| `void testReadBitShort_256()` | opcode=11 → 256 반환 검증 |
| `void testReadBitShort_uchar()` | opcode=01 + 1바이트 → 해당 값 검증 |
| `void testReadBitShort_short()` | opcode=00 + 2바이트 → little-endian 값 검증 |
| `void testReadModularChar_positive()` | 스펙 §2.6 예시 4610 검증 |
| `void testReadModularChar_negative()` | 스펙 §2.6 예시 -1413 검증 |
| `void testReadModularChar_large()` | 스펙 §2.6 예시 112823273 검증 |
| `void testReadBitDouble_zero()` | opcode=10 → 0.0 검증 |
| `void testReadBitDouble_one()` | opcode=01 → 1.0 검증 |
| `void testReadHandle()` | handle 인코딩/디코딩 라운드트립 |
| `void testReadVariableText_R2004()` | R2004: BS길이 + RC 배열 → String |
| `void testReadVariableText_R2007()` | R2007: BS길이 + UTF-16LE → String |
| `void testSeek()` | seek 후 위치 및 읽기값 검증 |
| `void testEofDetection()` | 스트림 끝 정확히 감지 |

---

### `BitStreamWriterTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testRoundTripBitShort()` | write 후 read → 원래 값 복원 |
| `void testRoundTripModularChar()` | 양수/음수/대수값 round-trip |
| `void testRoundTripBitDouble()` | 0.0/1.0/임의값 round-trip |
| `void testRoundTripHandle()` | 핸들 write/read 동일값 검증 |
| `void testRoundTripUnicodeText()` | 한글/특수문자 포함 TV round-trip |

---

### `ModularCharTest`

스펙 §2.6 표준 예시 3개를 각각 단위 테스트.

---

### `CrcCalculatorTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testCrc32KnownValue()` | 알려진 데이터의 CRC32 결과 검증 |
| `void testCrc8KnownValue()` | 알려진 데이터의 CRC8 결과 검증 |
| `void testSentinelInversion()` | invertSentinel 비트 반전 검증 |

---

### `Lz77Test`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testDecompressKnownData()` | 미리 계산된 압축 데이터 해제 후 원본과 비교 |
| `void testRoundTrip()` | compress → decompress → 원본 일치 |
| `void testLargeBuffer()` | 32KB 이상 버퍼 처리 |

---

### `DwgVersionDetectorTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testDetectAllSupportedVersions()` | 각 버전 문자열 → enum 정확히 감지 |
| `void testDetectUnsupportedThrows()` | "AC9999" → DwgVersionException |
| `void testIsDwgFile()` | "AC10"으로 시작하는지 확인 |

---

## 패키지: `io.dwg.test.section`

---

### `HeaderSectionParserTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testParseKnownHeader()` | 알려진 헤더 바이트 → 특정 변수값 검증 |
| `void testRoundTrip()` | parse → write → parse → 동일값 |
| `void testSentinelValidation()` | Sentinel 손상 시 예외 발생 |

---

### `ClassesSectionParserTest`, `HandlesSectionParserTest`, `ObjectsSectionParserTest`

각각 동일 패턴: known-data 파싱 검증 + round-trip + 손상 데이터 graceful 처리.

---

## 패키지: `io.dwg.test.integration`

---

### `DwgRoundTripTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testR2004RoundTrip()` | R2004 샘플 파일 read → write → read → 엔티티 수/핸들 일치 |
| `void testAllVersionRoundTrip()` | 각 버전별 샘플 파일 round-trip |
| `void testRoundTripPreservesHandles()` | 핸들 값이 변환 전후 동일한지 |
| `void testRoundTripPreservesXData()` | XData 손실 없는지 |

---

### `DwgEntityIntegrityTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testLineCoordinatesPreserved()` | LINE 엔티티 좌표 정밀도 손실 없음 |
| `void testCircleRadiusPrecision()` | CIRCLE 반지름 double 정밀도 검증 |
| `void testTextEncoding()` | 한글/일어/특수문자 포함 TEXT 손실 없음 |
| `void testBlockInsertChain()` | INSERT → BLOCK_HEADER → 엔티티 체인 핸들 해석 정확성 |
| `void testLayerAssignment()` | 각 엔티티의 레이어 핸들이 올바른 레이어를 가리키는지 |

---

### `DwgFuzzTest`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testRandomCorruption()` | 랜덤 바이트 변조 후 파싱 시 예외만 발생하고 JVM 크래시 없음 |
| `void testTruncatedFile()` | 파일 중간 잘림 → DwgCorruptedException graceful 처리 |
| `void testZeroLengthSection()` | 크기=0 섹션 처리 |
| `void testMaxHandleValue()` | Long.MAX_VALUE 핸들 처리 |

---

### `DwgSampleFileTest`

실제 AutoCAD 생성 DWG 파일들로 수행하는 호환성 테스트.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `void testOpenAcadSampleFiles()` | AutoCAD 샘플 파일 전체 오류 없이 파싱 |
| `void testEntityCountMatchesExpected()` | 알려진 파일의 엔티티 수 검증 |
| `void testLayerCountMatchesExpected()` | 알려진 파일의 레이어 수 검증 |

---

## 패키지: `io.dwg.test.fixture`

---

### `DwgTestFixtures`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static byte[] minimalR2004File()` | 최소 유효 R2004 DWG 바이트 배열 반환 |
| `static byte[] minimalR2007File()` | 최소 유효 R2007 DWG 바이트 배열 반환 |
| `static Path sampleFilePath(String name)` | test/resources 내 샘플 파일 경로 반환 |

---

### `BitPatternFactory`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static byte[] forBitShort(int value)` | BitShort 인코딩된 바이트 배열 생성 |
| `static byte[] forModularChar(int value)` | ModularChar 인코딩된 바이트 배열 생성 |
| `static byte[] forHandle(long handle)` | 핸들 인코딩 바이트 배열 생성 |

---

### `SampleEntityFactory`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `static DwgLine simpleLine()` | 핸들=1, (0,0,0)→(100,100,0) 기본 선 반환 |
| `static DwgCircle simpleCircle()` | 핸들=2, 중심(0,0,0), 반지름=50 원 반환 |
| `static DwgLayerEntry defaultLayer()` | "0" 레이어, 흰색, Continuous 선종류 반환 |
| `static DwgDocument simpleDocument(DwgVersion ver)` | 기본 레이어+선+원 포함 최소 문서 반환 |

---

## 패키지: `io.dwg.benchmark`

---

### `DwgReadBenchmark`

JMH 기반 읽기 성능 측정.

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `@Benchmark void readSmallFile()` | 엔티티 1,000개 미만 파일 처리 시간 |
| `@Benchmark void readMediumFile()` | 엔티티 50,000개 파일 처리 시간 |
| `@Benchmark void readLargeFile()` | 엔티티 500,000개 이상 파일 처리 시간 |
| `@Setup void loadFiles()` | 벤치마크 시작 전 파일 바이트 배열 메모리 로드 |

---

### `DwgWriteBenchmark`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `@Benchmark void writeSmallDocument()` | 소형 문서 직렬화 시간 |
| `@Benchmark void writeMediumDocument()` | 중형 문서 직렬화 시간 |

---

### `BitStreamBenchmark`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `@Benchmark void readBitShortThroughput()` | BitShort 1M회 읽기 처리량 |
| `@Benchmark void readModularCharThroughput()` | ModularChar 1M회 읽기 처리량 |

---

### `LargeFileBenchmark`

| 메서드 시그니처 | 처리 내용 |
|---|---|
| `@Benchmark void fullPipelineR2004()` | R2004 대용량 파일 전체 파이프라인 end-to-end 시간 |
| `@Benchmark void fullPipelineR2007()` | R2007 (LZ77 포함) 대용량 파일 전체 시간 |

---

---

# 상세 설계 보충

---

## DwgReader 파싱 파이프라인 상세

### open(Path filePath) 단계별 흐름

```
DwgReader.open(Path):

① 파일 읽기
    bytes = Files.readAllBytes(filePath)
    input = new ByteBufferBitInput(bytes)

② 버전 감지
    version = DwgVersionDetector.detect(bytes)
    if (!config.supportedVersions.contains(version)):
        throw DwgVersionException

③ 포맷 핸들러 획득
    handler = DwgFileStructureHandlerFactory.forVersion(version)

④ 파일 헤더 파싱
    fileHeader = handler.readHeader(input)

⑤ 섹션 맵 구성 (Map<String, SectionInputStream>)
    sections = handler.readSections(input, fileHeader)
    // 각 SectionInputStream은 해당 섹션의 압축 해제된 raw bytes를 보유

⑥ 섹션별 파싱 (순서 중요)
    a. classRegistry = ClassesSectionParser.parse(sections["AcDb:Classes"], version)
    b. handleRegistry = HandlesSectionParser.parse(sections["AcDb:Handles"], version)
    c. headerVars     = HeaderSectionParser.parse(sections["AcDb:Header"], version)
    d. objectMap      = ObjectsSectionParser.parse(
                            sections["AcDb:AcDbObjects"], version,
                            handleRegistry, classRegistry)
    e. [선택 사항, config.loadPreview == true일 때]
       preview = PreviewSectionParser.parse(sections["AcDb:Preview"], version)
    f. [R2004+ 전용]
       summaryInfo = SummaryInfoParser.parse(sections["AcDb:SummaryInfo"], version)
    g. [존재할 때만]
       auxHeader   = AuxHeaderParser.parse(sections["AcDb:AuxHeader"], version)

⑦ DwgDocument 조립
    doc = new DwgDocument(version, headerVars, objectMap,
                          handleRegistry, classRegistry, summaryInfo, preview)

⑧ 리스너 콜백 (config에 listener 등록 시)
    listener.onComplete(doc)

return doc
```

### 파싱 진행 이벤트 발생 시점

```
섹션 파싱 시작 시:    listener.onProgress(new ParseProgressEvent(sectionName, completed, total, ratio))
미지 엔티티 발견 시:  listener.onWarning(new ParseWarningEvent(message, bitOffset, context))
엔티티 1개 완료 시:   listener.onEntityParsed(new EntityParsedEvent(handle, type, totalParsed))
전체 완료 시:         listener.onComplete(document)
```

---

## DwgDocument 조립 로직

### entities() 동작

```java
List<DwgEntity> entities() {
    // *MODEL_SPACE 블록 헤더 조회
    DwgBlockHeader modelSpace = blocks().stream()
        .filter(DwgBlockHeader::isModelSpace)
        .findFirst()
        .orElseThrow();

    // firstEntity → lastEntity 핸들 체인 순회
    return collectEntitiesFromChain(
        modelSpace.firstEntity(), modelSpace.lastEntity());
}

private List<DwgEntity> collectEntitiesFromChain(DwgHandleRef first, DwgHandleRef last) {
    List<DwgEntity> result = new ArrayList<>();
    long currentHandle = first.rawHandle();

    while (currentHandle != 0) {
        DwgObject obj = objectMap.get(currentHandle);
        if (obj instanceof DwgEntity entity) {
            result.add(entity);
        }
        // noLinks=false인 경우 prev/next 핸들로 체인; true이면 핸들 순서로 추론
        if (currentHandle == last.rawHandle()) break;
        currentHandle = nextEntityHandle(currentHandle);
    }
    return result;
}
```

### layers() 동작

```java
List<DwgLayerEntry> layers() {
    // LAYER_CONTROL 객체 조회 (handle은 헤더 변수 CLAYER 참조 또는 타입으로 탐색)
    return objectsOfType(DwgLayerEntry.class);
}
```

### objectByHandle() 동작

```java
<T extends DwgObject> Optional<T> objectByHandle(long handle, Class<T> type) {
    DwgObject obj = objectMap.get(handle);
    if (obj == null) return Optional.empty();
    if (!type.isInstance(obj)) return Optional.empty();
    return Optional.of(type.cast(obj));
}
```

---

## DwgDocumentBuilder 핸들 자동 할당 전략

```java
DwgDocumentBuilder addEntity(DwgEntity entity) {
    // 핸들이 0(미설정)이면 자동 할당
    if (entity.handle() == 0) {
        entity.setHandle(nextHandle++);  // 순차 증가
    }

    // *MODEL_SPACE BLOCK_HEADER의 entity chain에 등록
    modelSpaceEntities.add(entity);
    objectMap.put(entity.handle(), entity);
    return this;
}

DwgDocument build() {
    // 1. 필수 비엔티티 자동 생성 (없는 경우)
    ensureBlockControl();      // BLOCK_CONTROL 생성
    ensureLayerControl();      // LAYER_CONTROL + "0" 레이어
    ensureModelSpaceBlock();   // *MODEL_SPACE BLOCK_HEADER
    ensurePaperSpaceBlock();   // *PAPER_SPACE BLOCK_HEADER

    // 2. 엔티티 체인 핸들 연결
    linkEntityChain(modelSpaceEntities);

    // 3. 레이어별 핸들 연결
    linkLayerHandles();

    // 4. HandleRegistry 구성 (핸들 → 오프셋은 실제 직렬화 후 계산)
    HandleRegistry registry = new HandleRegistry();
    for (DwgObject obj : objectMap.values()) {
        registry.put(obj.handle(), UNRESOLVED_OFFSET);
    }

    return new DwgDocument(version, headerVars, objectMap,
                           registry, classRegistry, summaryInfo, null);
}
```

---

## DwgWriter 직렬화 파이프라인

```
DwgWriter.write(DwgDocument doc, Path file):

① 섹션 직렬화
    headerBytes  = HeaderSectionWriter.write(doc.header(), targetVersion)
    classesBytes = ClassesSectionWriter.write(doc.customClasses, targetVersion)
    objectsMap   = ObjectsSectionWriter.write(doc.objectMap, targetVersion)
    // objectsMap: 직렬화 중 각 객체의 파일 내 오프셋 계산

② HandleRegistry 재구성 (오프셋 갱신)
    for each (handle, byteOffset) in objectsMap:
        handleRegistry.put(handle, byteOffset)
    handlesBytes = HandlesSectionWriter.write(handleRegistry, targetVersion)

③ 보조 섹션 직렬화 (R2004+)
    summaryBytes = SummaryInfoParser.write(doc.summaryInfo, targetVersion)

④ 포맷 핸들러로 파일 구조 조립
    handler = DwgFileStructureHandlerFactory.forVersion(targetVersion)
    sections = Map.of(
        "AcDb:Header", headerBytes,
        "AcDb:Classes", classesBytes,
        "AcDb:Handles", handlesBytes,
        "AcDb:AcDbObjects", objectsBytes,
        ...
    )
    FileHeaderFields fh = buildFileHeader(sections, targetVersion)
    output = new ByteBufferBitOutput()
    handler.writeHeader(output, fh)
    handler.writeSections(output, sections, fh)

⑤ 파일 저장
    Files.write(file, output.toByteArray())
```

---

## DwgQueryBuilder 내부 구현 상세

### 필터 체인 방식

```java
// 내부 필터 리스트 (AND 조건 결합)
private final List<Predicate<DwgEntity>> filters = new ArrayList<>();

DwgQueryBuilder onLayer(String layerName) {
    filters.add(entity -> {
        // entity의 layerHandle → DwgLayerEntry 조회 → name 비교
        return document.objectByHandle(entity.layerHandle().rawHandle(), DwgLayerEntry.class)
            .map(l -> l.name().equals(layerName))
            .orElse(false);
    });
    return this;
}

DwgQueryBuilder inBoundingBox(Point3D min, Point3D max) {
    filters.add(entity -> computeBBox(entity).intersects(min, max));
    return this;
}

List<DwgEntity> execute() {
    return document.entities().stream()
        .filter(e -> filters.stream().allMatch(f -> f.test(e)))
        .collect(Collectors.toList());
}
```

### Bounding Box 계산 전략

`inBoundingBox()` 필터의 `computeBBox()`:

| 엔티티 타입 | BBox 계산 방법 |
|---|---|
| `DwgLine` | `(min(start,end), max(start,end))` |
| `DwgCircle` | `center ± radius` |
| `DwgArc` | 호의 4분점 포함 계산 |
| `DwgText` | 삽입점 기반 (정확한 크기 미지원, 삽입점 주변 height × widthFactor) |
| `DwgInsert` | 블록 범위 × scale + insertionPoint |
| `DwgLwPolyline` | 모든 vertices의 min/max |

---

## DwgConverter 버전 변환 규칙

### 업그레이드 (하위 → 상위 버전)

```
R13 → R2004:
  - 텍스트 인코딩: CodePage → CodePage 유지
  - 핸들 구조: 동일
  - 섹션 구조: 재조립 (고정 오프셋 → PageMap/SectionMap)

R2004 → R2007:
  - 텍스트: CodePage → UTF-16LE 재인코딩 (TV 타입 전체)
  - 섹션: LZ77 압축 추가
  - 헤더: 암호화 방식 변경
```

### 다운그레이드 불가 케이스

```
DwgVersionConverter.canConvert(from, to):
  - from > to (다운그레이드) 일 때:
    R2007 → R2004: 가능 (UTF-16 → CodePage 변환, 손실 가능)
    R2018 → R2013: 가능
    R2007 → R13:   불가 (구조 변환 비용 너무 큼)
    → conversionWarnings()에서 경고 반환
```

---

## UnknownEntityPolicy 동작 상세

```
ObjectsSectionParser.parseOneObject():

ObjectReader reader = typeResolver.resolve(typeCode)
if reader == null:
    switch (config.unknownEntityPolicy):
        SKIP:
            // objectSize 비트만큼 seek 후 다음 객체로 이동
            bitReader.seek(bitReader.position() + objectSize)
            continue

        THROW:
            throw new DwgParseException(
                "Unknown entity type: " + typeCode, bitReader.position())

        WRAP_AS_PROXY:
            // 남은 데이터를 entityData byte[]로 보존
            DwgProxyEntity proxy = new DwgProxyEntity()
            proxy.setClassId(typeCode)
            proxy.setEntityData(readRemainingBytes(bitReader, objectSize))
            return proxy
```

---

## 테스트 픽스처 최소 유효 DWG 파일 구조

### minimalR2004File() 구성

최소 유효 R2004 DWG가 갖춰야 할 섹션:

```
필수 섹션:
  AcDb:Header        - 기본 헤더 변수 (INSBASE, EXTMIN, EXTMAX 등)
  AcDb:Classes       - 빈 클래스 섹션 (0개 클래스)
  AcDb:Handles       - 핸들 맵 (최소: 5개 핸들 - BLOCK_CONTROL, *MODEL_SPACE, *PAPER_SPACE, LAYER_CONTROL, LAYER "0")
  AcDb:AcDbObjects   - 5개 기본 객체

최소 객체 집합:
  handle=1: BLOCK_CONTROL (블록 테이블 루트)
  handle=2: BLOCK_HEADER "*MODEL_SPACE"
  handle=3: BLOCK_HEADER "*PAPER_SPACE"
  handle=4: LAYER_CONTROL
  handle=5: LAYER "0" (흰색, Continuous)
```

### BitPatternFactory 사용 예시

```java
// BS 값 4610 테스트용 바이트 배열 생성
byte[] bs4610 = BitPatternFactory.forBitShort(4610);
// → opcode 00 + RS(4610) = 0x00(비트 포함) 0x02 0x12

// ModularChar 4610 인코딩
byte[] mc4610 = BitPatternFactory.forModularChar(4610);
// → [0x22, 0xE2, 0x02]

// Handle (절대 핸들 0xDEAD) 인코딩
byte[] h = BitPatternFactory.forHandle(0xDEAD);
// → code=0x0, counter=0x2, bytes=[0xDE, 0xAD]
// → 4bit+4bit+16bit = 0x02 0xDE 0xAD
```

---

## 예외 처리 규약 요약

| 상황 | 발생 예외 | 처리 주체 |
|---|---|---|
| 지원하지 않는 버전 문자열 | `DwgVersionException` | `DwgVersionDetector.fromString()` |
| Sentinel 불일치 | `DwgCorruptedException` | `SentinelValidator.validate()` |
| CRC 검증 실패 (strictMode) | `DwgCorruptedException` | 각 포맷 핸들러 |
| 스트림 끝 넘어서 읽기 | `DwgParseException(bitOffset)` | `ByteBufferBitInput.readBit()` |
| 알 수 없는 타입 코드 | 정책에 따라 skip/exception/proxy | `ObjectsSectionParser` |
| 알 수 없는 섹션 이름 조회 | `UnknownSectionException` | `SectionParserRegistry.get()` |
| 쓰기 중 핸들 충돌 | `DwgWriteException` | `ObjectsSectionWriter` |
| 지원 불가 버전 변환 | `DwgWriteException` | `DwgVersionConverter.convert()` |
