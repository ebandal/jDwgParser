# TODO

목표: 서버에서 쓰는 순수 Java DWG **읽기** 라이브러리로, 상용 라이선스를 팔 수 있는 상태로 만든다.
위에서부터 순서대로 진행한다.

## P0 — 판매 전에 반드시 (라이선스)

- [ ] **libredwg에서 옮겨 온 코드를 ODA 스펙만 보고 다시 작성** (상용 라이선스를 따로 팔 권리를 확보하기 위해)
  - [x] `core/util/R2004Lz77Decompressor` — R2004~R2018 섹션 압축 해제 (스펙 §4.7). 샘플 244개에서 이전 구현과 결과 동일(호출 2,518회, 불일치 0), 무작위 입력 20만 건에서 예외 없음
  - [x] `core/util/Lz77Decompressor` — R2007 압축 해제 (스펙 §5.10). 샘플 244개에서 이전 구현과 결과·예외 동일(호출 3,313회, 불일치 0), 무작위 입력 20만 건에서 런타임 예외 없음. 쓰지 않던 `Lz77Decompressor_Fixed`, `ByteLevelLz77Decompressor`, `Lz77DecompressorInterface` 삭제
  - [x] `core/util/ReedSolomonDecoder` — R2007 RS 디코딩 (스펙 §5.13). 인터리브 해제만 함. `ReedSolomon251Decoder`, `ReedSolomonDecoderApache`와 이를 쓰던 디버그 클래스 3개 삭제. R2007 섹션 1,244개의 해시가 변경 전과 동일
  - [x] `format/r2007/R2007SystemPageReader` (스펙 §5.3)
  - [ ] `format/r2007/R2007SectionMapParser`
  - [ ] (선택) RS 오류 정정: 교재(Lin & Costello, *Error Control Coding*)와 Berlekamp-Massey·Chien·Forney 논문을 보고 직접 구현. 샘플 R2007 데이터 블록 303,887개에서 오류가 있는 블록은 0개였으므로 급하지 않음
  - [ ] `core/io/BitStreamReader` (UMC, BOT, BLL), `sections/objects/EntityHeaderReader`, `sections/handles/HandlesParsingUtil` — 주석이 libredwg 함수를 출처로 적고 있음. 스펙 기준으로 다시 확인하고 출처 주석은 스펙 절 번호로 바꾼다
  - [ ] 나머지 리더의 "per libredwg" 주석 정리 (필드 순서 같은 형식 정보는 스펙 절 번호로 출처를 바꾼다)
- [ ] 쓰지 않는 예전 패키지 삭제: `src/decode`, `src/structure`, `src/run`, `src/debug` (207개 파일, `io.dwg`에서 쓰지 않음. `decode/util/ReedSolomon.java`는 libredwg 포팅이라고 적혀 있음)
- [ ] 다시 작성하는 과정을 기록으로 남기기 (어떤 스펙 절을 보고 썼는지, 커밋 단위로)
- [ ] 라이선스 변경: GPLv3 → **AGPLv3 + 상용 라이선스** (듀얼). 위의 포팅 코드를 모두 다시 쓴 뒤에 진행. 판매 전에 변호사 확인
- [ ] 외부 기여를 받기 전에 CLA 준비

## P1 — 읽기 품질 (팔 수 있는 핵심)

- [ ] EED(XData)를 버리지 않고 저장 (`EntityHeaderReader.java:47`)
- [ ] 테이블 객체 필드를 모두 읽기: LTYPE(dash 배열), STYLE, DIMSTYLE, MLINESTYLE, xref 플래그
- [ ] XRECORD 데이터, 3DSOLID/REGION/BODY의 ACIS 데이터 읽기 (적어도 원본 바이트를 보관)
- [ ] MULTILEADER 리더 추가
- [ ] 알 수 없는 클래스를 `null` 대신 원본 바이트를 담은 객체로 반환 (`R2000ObjectStreamAdvancedParser.java:153`)
- [ ] 한글: CP949 텍스트, MTEXT 제어코드, SHX/빅폰트 확인
- [ ] 경고를 stdout에 출력하지 말고 리스너나 로거로 넘기기 (`DictionaryObjectReader` 경고가 대량으로 출력됨 → 원인도 조사)

## P2 — 신뢰성 수치

- [ ] 실제 도면 수백~수천 개로 테스트하고 성공률 공개
- [ ] 퍼징: 예외, 무한 루프, 메모리 폭주가 없어야 함. 읽기 타임아웃과 메모리 상한 옵션 제공
- [ ] 벤치마크 (JMH): 속도와 메모리
- [ ] `src/io/dwg/test`의 디버그용 main 클래스를 정리하고 JUnit 테스트로 옮기기 (`DwgRoundTripTest`에는 assert가 없음)

## P3 — 도입하기 쉽게

- [ ] 루트 정리: `PHASE_*.md`, `SESSION_*.md`, `test_*.java`, `Analyze*.java` 등을 지우거나 `doc/history`로 옮기기
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

## 렌더링에서 아직 안 되는 것

3DSOLID/REGION, 폴리곤 메시, MINSERT, MULTILEADER, ATTDEF, VIEWPORT, 이미지, 선종류, 선 굵기, 폴리라인 폭
