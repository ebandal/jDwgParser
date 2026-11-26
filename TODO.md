# TODO

목표: 서버에서 쓰는 순수 Java DWG **읽기** 라이브러리로, 상용 라이선스를 팔 수 있는 상태로 만든다.
위에서부터 순서대로 진행한다.

## P0 — 판매 전에 반드시 (라이선스)

- [x] **다른 구현에서 옮겨 온 코드를 ODA 스펙만 보고 다시 작성** (상용 라이선스를 따로 팔 권리를 확보하기 위해)
  - [x] `core/util/R2004Lz77Decompressor` — R2004~R2018 섹션 압축 해제 (스펙 §4.7). 샘플 244개에서 이전 구현과 결과 동일(호출 2,518회, 불일치 0), 무작위 입력 20만 건에서 예외 없음
  - [x] `core/util/Lz77Decompressor` — R2007 압축 해제 (스펙 §5.10). 샘플 244개에서 이전 구현과 결과·예외 동일(호출 3,313회, 불일치 0), 무작위 입력 20만 건에서 런타임 예외 없음. 쓰지 않던 `Lz77Decompressor_Fixed`, `ByteLevelLz77Decompressor`, `Lz77DecompressorInterface` 삭제
  - [x] `core/util/ReedSolomonDecoder` — R2007 RS 디코딩 (스펙 §5.13). 인터리브 해제만 함. `ReedSolomon251Decoder`, `ReedSolomonDecoderApache`와 이를 쓰던 디버그 클래스 3개 삭제. R2007 섹션 1,244개의 해시가 변경 전과 동일
  - [x] `format/r2007/R2007SystemPageReader` (스펙 §5.3)
  - [x] `format/r2007/R2007SectionMapParser` (스펙 §5.2). 페이지 수 상한 1,000을 없애고 남은 데이터 길이로 검사 (큰 파일에서 섹션이 잘리던 문제 방지)
  - [x] R2007: 페이지를 섹션 맵의 오프셋에 원래 크기만큼 넣음(§5.5). 파일에 없는 페이지(§5.2)는 0으로 남음. 압축하지 않은 페이지를 버리거나 길게 붙이던 버그도 함께 고침(아래 villa_1)
  - [ ] (선택) RS 오류 정정: 교재(Lin & Costello, *Error Control Coding*)와 Berlekamp-Massey·Chien·Forney 논문을 보고 직접 구현. 샘플 R2007 데이터 블록 303,887개에서 오류가 있는 블록은 0개였으므로 급하지 않음. 데이터 페이지의 매개변수는 샘플로 확인함: GF(256), p(x) = 1 + x² + x³ + x⁴ + x⁸, 근 α¹~α⁴, 부호어의 첫 바이트가 x⁰의 계수(신드롬 계산으로 확인, `jDwgParser-records/tools/rs_syndromes.py`)
  - [x] `core/io/BitStreamReader`: 스펙 2장을 보고 다시 작성. 쓰지 않는 `readBitDoubleWithDefault` 삭제
  - [ ] BLL(§2.4) 길이 읽는 방식을 실제 파일로 확인: 스펙은 3B라고 하지만 3B로는 1·3·4·5바이트를 표현할 수 없어 3비트 숫자로 읽고 있음. 샘플에는 이 값을 쓰는 엔티티(R2010+ 미리보기)가 없음
  - [x] 파일 구조 처리기: R13~R2000 헤더(`R13FileHeaderReader`, §3.2), 섹션 로케이터(§3.2.6), R2004 헤더와 복호화(§4.1), R2007 헤더(§5.2)를 스펙을 보고 다시 작성. 중복이던 `R2007SectionMap`, 샘플에서 쓰이지 않던 R2004 페이지 순차 탐색 대체 경로 삭제
  - [x] 객체 맵(`HandlesParsingUtil`, 23장): R13과 R2000+를 하나의 구현으로. 섹션마다 핸들·위치를 0부터 다시 세는 것이 맞음을 측정으로 확인 (한 번만 초기화하면 223만 → 10만 객체)
  - [x] `ObjectsSectionParser`: 객체 시작 형식(§20.1) 주석 정리, 샘플에서 쓰이지 않던 순차 탐색 대체 경로 삭제
  - [x] `EntityHeaderReader`: 스펙 §20.1·§20.2·§20.4.1·28장을 보고 다시 작성. R2007+ 테이블 레코드 플래그는 BS 하나뿐임을 측정으로 확인 (스펙대로 읽으면 R2007~R2018 버전 비교 테스트 4개 실패)
  - [x] TEXT·ATTRIB·ATTDEF·MTEXT·3DFACE·LWPOLYLINE·SPLINE 리더: 스펙 §20.4를 보고 다시 작성. 버그 2건 수정: LWPOLYLINE R2010+ 정점 ID, R13 ATTDEF(R13 이전 형식으로 읽고 있었음)
  - 검증 방법: 재작성마다 샘플 244개의 모든 객체 필드를 덤프해 변경 전과 비교 (스크립트는 `D:\workspace\jDwgParser-records`)
  - 저장소 파일에 다른 구현을 참고한 흔적 없음 (2026-11-25, git 이력 제외)
- [ ] 스펙에 없거나 스펙과 다른데 샘플로 확인하지 못한 것 (확인할 파일이 필요함)
  - [ ] ENC 색상(R2004+): 투명도를 RGB보다 먼저 읽음. R2004~R2007에서 색상 핸들(0x40)을 본문 스트림에서 읽음 (스펙은 핸들 스트림). 샘플에 0x40이 있는 엔티티 71개, 투명도+RGB가 함께 있는 것 24개
  - [ ] R2018 여러 줄 속성(`skipMTextEmbedded`): 샘플에 없음
  - [ ] LWPOLYLINE 정점 ID + 폭: 정점 ID가 있는 것은 6개지만 폭이 함께 있는 것은 없음
  - [ ] EED 레이어 핸들(1003): 샘플에 없음. 엔티티 핸들(1005)과 같은 형식으로 읽음
  - [ ] LTYPE 텍스트: R2004 이전 파일에서 텍스트 위치(바이트 오프셋으로 읽음)와, R2007+에서 텍스트 없이 모양만 있는 경우 텍스트 영역 유무. 샘플에 없음
  - [ ] CMC 트루컬러(방식 0xC2 등): 샘플에 없음. 인덱스 필드를 그대로 둠
- 스펙과 다르게 구현한 곳 (샘플로 측정해서 정함)
  - DIMSTYLE: R2004+에서 DIMCLRD·DIMCLRE·DIMCLRT는 BS가 아니라 CMC
  - LTYPE: 모양 플래그 2가 텍스트, 4가 모양 (스펙은 반대). R2007+ 텍스트 위치는 문자 단위 인덱스
  - R2007+ 테이블 레코드 플래그: BS 하나뿐이고 256이 xref 종속. 64-플래그와 xref 인덱스는 없음
  - LAYER: 스펙의 마지막 알 수 없는 핸들은 R2013+에만 있음 (534개 모두 null)
  - CMC(R2004+): RGB 값의 최상위 바이트 0xC0은 ByLayer, 0xC1은 ByBlock, 0xC3은 하위 바이트가 색상 인덱스
  - Classes: R2010+ 비트 크기는 알 수 없는 RL 다음부터 셈. 그 RL은 R2010/R2013 유지보수 버전 > 3, R2018+에 있음
  - XRECORD: R13~R2004 문자열 길이는 RS (스펙은 RC), 이진 데이터 길이는 RC
  - MTEXT: 배경 배율은 BD (스펙은 BL)
  - INSERT: R2004+ 소유 객체 수는 ATTRIB이 있을 때만 있음 (스펙은 항상)
  - LAYOUT: R2004+ 뷰포트 수는 BL (스펙은 RL)
  - DICTIONARYVAR: 문자열은 TV (스펙은 BS)
  - VIEW: R2000+ UCS 핸들 2개는 연결된 UCS가 있을 때만 (샘플 1개로 확인)
  - ACIS: 버전 1 문자는 0x9F에서 뺀 값이 0x20~0x7E일 때만 바뀜(공백은 0x7F로 저장). 실루엣의 와이어 수 앞에 비트가 하나 더 있음. R2007+ REGION에는 history 핸들이 없음
  - EED·XRECORD 그룹 1003/1005 핸들: 샘플에 없음
  - WIPEOUT은 IMAGE와 같은 구성. HATCH R2010+ 스플라인 접선은 맞춤점이 있을 때만. TOLERANCE R13~R14 알 수 없는 short는 BS, 텍스트는 TV. FIELD 값 문자열 길이는 BL
  - TABLESTYLE: 옛 형식의 색은 TC, R2010+ 셀 스타일 목록의 각 항목 앞에 BL 하나. CELLSTYLEMAP은 스펙대로
  - ACAD_TABLE: R2010+ 형식에도 INSERT 필드와 INSERT 핸들이 앞에 있음. 셀 내용의 형식 재정의 앞에 BL 플래그. 값(§20.4.99)의 문자열은 R2007+에서 TV가 아니라 BL 바이트 수 + UTF-16LE 바이트. R2007 셀에는 텍스트 TV가 없음(텍스트는 값에)
  - MULTILEADER: 끊김 정보는 개수만큼 반복, 스타일 블록 핸들은 블록 내용일 때만, 내장 컨텍스트에 AcDbObjectContextData 필드 없음
  - ACIS R2013+: 모델은 AcDs 섹션에 엔티티 핸들로 있고 empty 비트가 1. 그 뒤 비트 하나 다음에 바로 와이어프레임 점(점 유무 비트 없음). 끝에 비트, GUID(BL, BS, BS, 8 RC), BL. R13: 와이어프레임 뒤 개수 하나(실루엣 수로 정함)가 없음
  - AcDs(24장): 데이터 색인의 로컬 오프셋은 세그먼트 머리말(48바이트) 뒤부터. R2018 SAB는 "ASM BinaryFile4"로 시작. LAYOUT·VIEW의 PNG 미리보기도 들어 있음
  - BLOCK_HEADER: R2004+ 소유 객체 수는 xref·overlay 블록에는 없음 (스펙은 항상)
- [x] 쓰지 않는 예전 패키지 삭제: `src/decode`, `src/structure`, `src/run`, `src/debug` (207개 파일)와 이를 쓰던 `src/test`의 예전 프로그램 4개
- [x] 문서 정리: `CLAUDE.md`의 조사 원칙을 "스펙 → 샘플 실험 → 일반 알고리즘은 문헌"으로 바꾸고 다른 구현의 소스는 보지 않도록 함. `README.md`, `RELEASE_NOTES.md`, `03_dwg-sections.md`는 스펙 절과 측정값을 출처로. 루트의 작업 기록 문서 23개 삭제
- [x] 다시 작성하는 과정을 기록으로 남기기: 비공개 기록부 `D:\workspace\jDwgParser-records\cleanroom-log.md`
- [ ] 라이선스 변경: GPLv3 → **AGPLv3 + 상용 라이선스** (듀얼). 위의 포팅 코드를 모두 다시 쓴 뒤에 진행. 판매 전에 변호사 확인
- [ ] 외부 기여를 받기 전에 CLA 준비

## P1 — 읽기 품질 (팔 수 있는 핵심)

- [x] EED(XData)를 버리지 않고 저장 (스펙 28장). `XDataRecord`에 DXF 그룹 코드로 담고, 1001은 애플리케이션 이름. 샘플에서 애플리케이션 데이터가 모두 길이에 딱 맞게 읽히고, 애플리케이션·엔티티 핸들이 파일의 객체를 가리킴
- [x] 테이블 객체 필드를 모두 읽기: LTYPE(dash 배열, 텍스트), STYLE, DIMSTYLE(변수 전체), MLINESTYLE(선 목록), APPID, LAYER(플롯, 선 굵기), BLOCK_HEADER(설명, 미리보기, 소유 엔티티·INSERT·레이아웃 핸들), 모든 테이블 레코드의 xref 플래그와 xref 블록 핸들(`AbstractDwgTableRecord`). 샘플 전체에서 데이터 스트림과 핸들 스트림이 모두 객체 끝에서 정확히 끝나고(아래 villa_1 제외), R2000 사본과 값이 같음
  - [x] VIEW, UCS, VPORT도 같은 방법으로 다시 작성 (필드는 이름-값으로 보관, `ValueHolder`)
- [x] 읽기 점검을 라이브러리에 넣음: 리더가 데이터 끝에서 멈추지 않거나 핸들이 남으면 `ReadIssue`(OBJECT_DATA/OBJECT_HANDLES)로 알림. `DwgReader.onIssue`, `DwgDocument.issues()`. 점검을 통과한 타입은 `ReadCompletenessTest`가 지킴
- [x] 경고를 출력하지 않고 `ReadIssue`로 넘김. 출력하던 예전 파서 3개 삭제. `DictionaryObjectReader` 경고의 원인은 리더가 공통 헤더를 두 번 읽던 것 (다시 작성)
- [x] Classes 섹션(10장, §5.8): 모든 클래스가 엔티티로 표시되던 버그, R2004+ 머리말·추가 필드, R2007+ 문자열 스트림. 샘플 전체에서 클래스 번호가 500부터 차례대로
- [x] DICTIONARY, XRECORD(데이터를 그룹 코드와 값으로), DICTIONARYVAR, SCALE, GROUP, LAYOUT, VIEWPORT
- [x] 엔티티: RAY, XLINE, DIMENSION 7종, MTEXT(배경, R2018 열), INSERT·MINSERT(ATTRIB 핸들), 옛 POLYLINE 4종(정점 핸들), ATTDEF, VERTEX_MESH
- [x] 3DSOLID/REGION/BODY: ACIS 모델을 SAT 텍스트나 SAB 바이트로 보관, 와이어프레임 읽기. R13~R2018 샘플 전부 정확
  - [x] R2013+: 데이터 저장 섹션(AcDs, 24장) 읽기(`DataStorageSectionParser`, `DwgDocument.dataStorage()`), 모델을 엔티티에 연결. GUID(`guid()`)
  - [x] R13 `example_r13.dwg`: SAT가 R14 사본과 같음
  - [ ] AcDs 블롭 참조(0x40000바이트 넘는 모델): 스펙대로 작성했지만 샘플에 없음
  - [ ] R13에서 빠진 개수가 와이어 수인지 실루엣 수인지 (샘플은 둘 다 0)
- [x] 알 수 없는 클래스: 원래부터 `DwgGenericObject`로 반환. 이제 클래스 이름, 엔티티 여부, 객체 바이트도 보관
- [x] SHAPE, TOLERANCE, MLINE, IMAGE, WIPEOUT, HATCH(R2010+ 스플라인), OLE2FRAME, FIELD, TABLESTYLE, CELLSTYLEMAP
- [x] MULTILEADER 리더 추가 (컨텍스트, 리더 루트·선, 텍스트·블록 내용). 샘플 15개 모두 정확
  - [ ] R2010+: 화살촉 목록과 블록 레이블 목록 중 어느 것이 있는지 (샘플은 둘 다 비어 있어 판정 못 함)
- [x] 스펙에 없는 타입(VISUALSTYLE, MATERIAL, PERSSUBENTMANAGER, SURFACE, MESH, UNDERLAY): 지어낸 필드를 읽던 리더 대신 공통 데이터만 읽고 바이트를 보관(`rawData()`)
- [x] ACAD_TABLE: R2007까지의 형식(셀, 재정의)과 R2010+ 형식(TABLECONTENT). 샘플 8개 모두 정확, 버전별 셀 텍스트 같음
- [x] ACAD_PROXY_ENTITY(§20.4.90): 지어낸 필드를 읽던 리더를 스펙대로 다시 작성(클래스 ID, 형식, 데이터 비트, 객체 핸들). villa_2의 1,463개 모두 클래스 ID가 Classes에 있고 핸들이 모두 실제 객체를 가리킴
- [x] 남은 `ReadIssue` 정리: gh44의 xref BLOCK_HEADER 2개, ACIS 6개, villa_2 프록시 1,463개
- [x] 읽기 점검에 R2007+ 문자열 스트림 추가: 끝을 넘으면 실패하고, 끝까지 읽어야 함. 이것으로 R2007 표가 셀마다 문자열을 하나 더 읽던 것을 찾음(R2007 셀에는 텍스트 TV가 없음). 이제 샘플 전체에서 `ReadIssue` 0개
- [x] Header 섹션(9장) 리더를 스펙 목록대로 다시 작성(`HeaderVariableTable`). CRC(§2.14.1)와 끝 표지 확인, R2007+는 세 스트림. 샘플 253개 중 251개가 모든 스트림 끝까지 정확, 헤더 핸들 10,167개가 모두 실제 객체
  - 스펙과 다른 곳: R2007+ 비트 크기는 모든 R2007+에 있음, R2010+ 크기 뒤 4바이트, MENUNAME은 모든 버전, 끝의 알 수 없는 short 4개는 R13에도 있고 R2007+에서는 핸들 뒤, R2007·R2010 끝에 0xAF 한 바이트
  - [ ] `example_r13.dwg`는 12비트, `2000/PolyLine2D.dwg`는 1바이트가 더 남음 (판정할 샘플 부족)
  - [ ] `CrcCalculator.Crc8Calculator`는 스펙 §2.14.1과 다른 계산임(새 `Crc16`이 스펙대로). 쓰는 곳 확인 후 정리
  - [ ] `HeaderSectionWriter`는 예전 순서로 씀 (쓰기 작업 때 표를 써서 다시 작성)
- [x] R2007 `AC1021/villa_1.dwg`, `bmw_1.dwg`, `mercedes_1.dwg`: 압축하지 않은 페이지가 RS 블록 단위로 길게 붙어 그 뒤 데이터가 228바이트 밀림. 작은 섹션(SummaryInfo 등)은 R2007 파일 94개에서 0으로만 채워져 있었음. 객체 크기가 섹션을 넘으면 건너뜀(villa_1의 메모리 부족)
- [x] R2007 `samples/2007/Constraints.dwg`: Classes 페이지의 LZ77 입력에 압축 크기 뒤의 채움 바이트가 섞여 실패했고, 그 실패가 Classes·AuxHeader·Header를 모두 버리게 했음. 함께 찾은 것: encoding 1 섹션(Preview 등)은 RS 부호어를 인터리브하지 않음(R2007 Preview 98개가 모두 뒤섞여 있었음)
- [ ] 한글: CP949 텍스트, MTEXT 제어코드, SHX/빅폰트 확인 — 보류(2026-11-26): 한글 도면 샘플을 구한 뒤 진행
  - 샘플에 한글 도면이 없음(코드페이지 949 파일 0개, 한글 문자가 있는 텍스트 0개). 한글 도면 샘플이 필요함
  - 텍스트 해석 자체는 맞음: R2007+ UTF-16, R2004 이전 코드페이지 29(windows-1251, 러시아어)와 30(windows-1252, °·Ø)이 샘플에서 바르게 나옴
  - [ ] 코드페이지 번호표(`DwgStringDecoder.charsetForDwgCodePage`, 949는 25·40번)는 2026-11-13 커밋에서 들어왔고 출처 기록이 없음. 스펙에는 번호표가 없음. 샘플로 확인된 것은 29·30번뿐. 출처를 정하거나 한글 샘플로 확인 필요

## P2 — 신뢰성 수치

- [x] 성공률 측정과 공개: [RELIABILITY.md](RELIABILITY.md). R13~R2018 샘플 233개 모두 열림, 객체 2,244,417개 중 문제 0 (`io.dwg.tools.SuccessReport`)
  - [ ] 샘플을 수천 개로 늘리기 (지금 233개). 특히 한글 도면과 R2010·R2018 큰 도면
- [x] 퍼징(`io.dwg.tools.ReadFuzz`, 회귀는 `RobustnessTest`): 메모리 부족 7건과 내부 예외를 고침. 6,000회에서 문서 또는 `DwgReadException`만 나옴
- [x] 읽기 상한: `DwgReader.maxBytes(long)`(기본 최대 힙의 절반), `timeout(Duration)`. `open()`은 `DwgReadException` 하나만 던짐
- [x] 벤치마크(JMH, `io.dwg.bench.ReadBenchmark`): 약 25 MB/s, 결과는 RELIABILITY.md
  - [x] 메모리: 문서가 붙잡는 힙을 파일 크기의 약 14배에서 6~7배로 줄임(14MB 도면 198MB → 91MB), 속도는 그대로. 빈 목록·드문 필드는 필요할 때만, 핸들 참조·색·최근 좌표 공유, 핸들 맵·객체 맵·블록 엔티티 목록·SPLINE 점과 매듭을 기본형 배열로, 작은 코드값은 byte
    - [ ] 더 줄일 곳: 선·정점 객체 자체(14MB 도면에서 28MB), 필드의 서로 다른 좌표(약 13MB). 좌표를 객체 안에 풀면 좌표 공유의 이득을 잃어 효과가 작음
- [x] `src/io/dwg/test`의 디버그용 main 클래스 79개 삭제: 모두 assert 없는 main이라 옮길 테스트가 없었음. `DwgParsingStageTest`(README·TESTING.md에 사용법이 있음)만 남김

## P3 — 도입하기 쉽게

- [x] 루트 정리: 루트의 작업용 파일 32개(Java 29, 결과 파일 2, 스크립트 1), `src`의 기본 패키지 파일 2개와 `src/test`(16개)·`src/target` 삭제 (모두 라이브러리 jar에 들어가고 있었음)
- [x] Java 11로 낮춤 (record 12개를 같은 API의 클래스로, switch 식·instanceof 패턴·텍스트 블록 제거). Java 8은 `var`·`List.of`·Java 11 API 때문에 하지 않음
- [x] 공개 API: `DwgDocument.objects(Class)`, `object(handle)`, `layerOf(entity)`. API 패키지의 package-info(나머지는 내부용이라고 밝힘), `Automatic-Module-Name: io.dwg`, Javadoc 오류 0, README 5줄 빠른 시작(`QuickStartTest`)
  - [ ] Javadoc 누락 경고(공개 메서드 설명 없음) 정리
- [x] ~~Maven Central 배포~~ — 하지 않음(2026-11-26): jar를 직접 판매
- [x] SVG, PDF 출력: `Renderer.renderSvg`, `renderPdf`. 외부 라이브러리 없이, PNG와 같은 그림(`VectorOutputTest`)

## P4 — 알리기

- [ ] 웹 데모: DWG를 올리면 렌더링하고 텍스트를 추출해 보여주기 — 보류(2026-11-26)
- [x] Apache Tika 파서 플러그인: `examples/tika-parser`. 텍스트(문단)와 메타데이터(버전, 객체 수, 문제 수). `AutoDetectParser`가 서비스 등록으로 사용
  - 바탕: `DwgDocument.texts()`(TEXT, ATTRIB, MTEXT, 표 셀, 다중 지시선), `PlainText`(서식 코드 제거). 버전별 사본에서 같은 텍스트
- [ ] Spring Boot 예제 (업로드 → 썸네일 → 색인) — 보류(2026-11-26)
- [ ] 잠재 고객 3~5명 인터뷰 (문서관리, 건설, 공공 SI) — 질문지·대상·판단 기준: `jDwgParser-records/business/interview-guide.md`
- 보류: LibreOffice 확장. 사용자가 무료로 쓰려는 층이라 수익으로 이어지지 않음

## 나중에 — 쓰기 (`DwgWriter`)

지금은 내세우지 않는다.

- [ ] R2010/R2013/R2018 저장 시 버전 문자열이 `AC1018`로 들어가는 문제 (`R2004FileStructureHandler.writeHeader`)
- [ ] 섹션 크기, CRC 임시값 (`HeaderSectionWriter.java:37`, `ClassesSectionWriter.java:44`, `R2000FileStructureHandler.java:211`)
- [ ] R2007 이상 페이지맵 오프셋을 추정이 아니라 계산으로
- [ ] 테이블 객체 writer
- [ ] DIMENSION, MINSERT writer: 읽기를 다시 쓰기 전의 잘못된 구성으로 씀
- [ ] 라운드트립 테스트
- [ ] `R13SectionLocator.write`가 레코드 번호를 raw long으로 씀 (§3.2.6은 raw byte). `DwgWriter`의 헤더 크기 계산도 함께 고칠 것

## 렌더링에서 아직 안 되는 것

3DSOLID/REGION, 폴리곤 메시, MINSERT, MULTILEADER, ACAD_TABLE(익명 블록 *T를 INSERT로 그리면 됨, 기준 렌더러도 함께 고쳐야 함), ATTDEF, VIEWPORT, 이미지, 선종류, 선 굵기, 폴리라인 폭
