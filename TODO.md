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
  - [ ] R2007: 스펙 §5.2의 "0으로만 된 페이지는 파일에 쓰지 않는다" 규칙 처리 (페이지 데이터 오프셋으로 빈 페이지 채우기). 지금은 페이지를 이어 붙이기만 함
  - [ ] (선택) RS 오류 정정: 교재(Lin & Costello, *Error Control Coding*)와 Berlekamp-Massey·Chien·Forney 논문을 보고 직접 구현. 샘플 R2007 데이터 블록 303,887개에서 오류가 있는 블록은 0개였으므로 급하지 않음
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
- [x] 쓰지 않는 예전 패키지 삭제: `src/decode`, `src/structure`, `src/run`, `src/debug` (207개 파일)와 이를 쓰던 `src/test`의 예전 프로그램 4개
- [x] 문서 정리: `CLAUDE.md`의 조사 원칙을 "스펙 → 샘플 실험 → 일반 알고리즘은 문헌"으로 바꾸고 다른 구현의 소스는 보지 않도록 함. `README.md`, `RELEASE_NOTES.md`, `03_dwg-sections.md`는 스펙 절과 측정값을 출처로. 루트의 작업 기록 문서 23개 삭제
- [x] 다시 작성하는 과정을 기록으로 남기기: 비공개 기록부 `D:\workspace\jDwgParser-records\cleanroom-log.md`
- [ ] 라이선스 변경: GPLv3 → **AGPLv3 + 상용 라이선스** (듀얼). 위의 포팅 코드를 모두 다시 쓴 뒤에 진행. 판매 전에 변호사 확인
- [ ] 외부 기여를 받기 전에 CLA 준비

## P1 — 읽기 품질 (팔 수 있는 핵심)

- [x] EED(XData)를 버리지 않고 저장 (스펙 28장). `XDataRecord`에 DXF 그룹 코드로 담고, 1001은 애플리케이션 이름. 샘플에서 애플리케이션 데이터가 모두 길이에 딱 맞게 읽히고, 애플리케이션·엔티티 핸들이 파일의 객체를 가리킴
- [x] 테이블 객체 필드를 모두 읽기: LTYPE(dash 배열, 텍스트), STYLE, DIMSTYLE(변수 전체), MLINESTYLE(선 목록), APPID, LAYER(플롯, 선 굵기), BLOCK_HEADER(설명, 미리보기, 소유 엔티티·INSERT·레이아웃 핸들), 모든 테이블 레코드의 xref 플래그와 xref 블록 핸들(`AbstractDwgTableRecord`). 샘플 전체에서 데이터 스트림과 핸들 스트림이 모두 객체 끝에서 정확히 끝나고(아래 villa_1 제외), R2000 사본과 값이 같음
  - [ ] VIEW, UCS, VPORT 리더도 같은 방법(데이터 끝 위치 확인)으로 점검
- [ ] XRECORD 데이터, 3DSOLID/REGION/BODY의 ACIS 데이터 읽기 (적어도 원본 바이트를 보관)
- [ ] MULTILEADER 리더 추가
- [ ] R2007 `AC1021/villa_1.dwg`, `bmw_1.dwg`: 객체 대부분이 잘못 읽힘(타입이 무작위, EED 애플리케이션 핸들이 APPID가 아님). 섹션이나 객체 맵 단계 문제로 보임
- [ ] 알 수 없는 클래스를 `null` 대신 원본 바이트를 담은 객체로 반환 (`R2000ObjectStreamAdvancedParser.java:153`)
- [ ] 한글: CP949 텍스트, MTEXT 제어코드, SHX/빅폰트 확인
- [ ] 경고를 stdout에 출력하지 말고 리스너나 로거로 넘기기 (`DictionaryObjectReader` 경고가 대량으로 출력됨 → 원인도 조사)

## P2 — 신뢰성 수치

- [ ] 실제 도면 수백~수천 개로 테스트하고 성공률 공개
- [ ] 퍼징: 예외, 무한 루프, 메모리 폭주가 없어야 함. 읽기 타임아웃과 메모리 상한 옵션 제공
- [ ] 벤치마크 (JMH): 속도와 메모리
- [ ] `src/io/dwg/test`의 디버그용 main 클래스를 정리하고 JUnit 테스트로 옮기기 (`DwgRoundTripTest`에는 assert가 없음)

## P3 — 도입하기 쉽게

- [ ] 루트 정리: `test_*.java`, `Analyze*.java` 같은 작업용 파일 (작업 기록 문서는 정리함)
- [ ] Java 11(가능하면 8)로 낮추기 검토 (지금은 16)
- [ ] 공개 API 정리, 내부 패키지 숨기기, 5줄 빠른 시작 예제, Javadoc
- [ ] Maven Central 배포
- [ ] SVG, PDF 출력 (렌더러에 추가)

## P4 — 알리기

- [ ] 웹 데모: DWG를 올리면 렌더링하고 텍스트를 추출해 보여주기
- [ ] Apache Tika 파서 플러그인
- [ ] Spring Boot 예제 (업로드 → 썸네일 → 색인)
- [ ] 잠재 고객 3~5명 인터뷰 (문서관리, 건설, 공공 SI)
- 보류: LibreOffice 확장. 사용자가 무료로 쓰려는 층이라 수익으로 이어지지 않음

## 나중에 — 쓰기 (`DwgWriter`)

지금은 내세우지 않는다.

- [ ] R2010/R2013/R2018 저장 시 버전 문자열이 `AC1018`로 들어가는 문제 (`R2004FileStructureHandler.writeHeader`)
- [ ] 섹션 크기, CRC 임시값 (`HeaderSectionWriter.java:37`, `ClassesSectionWriter.java:44`, `R2000FileStructureHandler.java:211`)
- [ ] R2007 이상 페이지맵 오프셋을 추정이 아니라 계산으로
- [ ] 테이블 객체 writer
- [ ] 라운드트립 테스트
- [ ] `R13SectionLocator.write`가 레코드 번호를 raw long으로 씀 (§3.2.6은 raw byte). `DwgWriter`의 헤더 크기 계산도 함께 고칠 것

## 렌더링에서 아직 안 되는 것

3DSOLID/REGION, 폴리곤 메시, MINSERT, MULTILEADER, ATTDEF, VIEWPORT, 이미지, 선종류, 선 굵기, 폴리라인 폭
