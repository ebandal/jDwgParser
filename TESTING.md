# DWG 파싱 단계별 테스트 가이드

## 개요

`DwgParsingStageTest`는 DWG 파일 파싱을 6개 단계로 나누어 각 단계별 성공/실패를 명확히 파악할 수 있는 테스트입니다.

각 단계에서 어느 과정에서 실패하는지를 명확히 출력하므로, 문제 해결이 용이합니다.

---

## 테스트 단계

### STAGE 1: 파일 읽기
- 샘플 DWG 파일을 바이트 배열로 읽음
- 파일 크기 확인

### STAGE 2: 버전 감지
- 파일 헤더(첫 6바이트)에서 DWG 버전 문자열 추출
- 지원 버전 여부 확인
- 예: `AC1021` → R2007

### STAGE 3: 헤더 파싱
- 파일 헤더(0x80 바이트)를 디코딩
- 메타 정보 추출:
  - 유지보수 버전
  - 코드페이지
  - 보안 플래그 (암호화 여부)
  - 섹션 오프셋

### STAGE 4: 섹션 추출
- 파일 구조 핸들러(R2004/R2007 등)로 섹션 맵 파싱
- 각 섹션의 위치와 크기 추출
- R2007+ 버전은 LZ77 압축 해제 포함
- 추출된 섹션 목록:
  - `AcDb:Header` - 헤더 변수
  - `AcDb:Classes` - 클래스 정의
  - `AcDb:Handles` - 핸들 맵
  - `AcDb:AcDbObjects` - 모든 객체

### STAGE 5: 개별 섹션 파싱
섹션별 파서로 각 섹션을 도메인 객체로 변환:

#### [5-1] Header 섹션
- `HeaderVariables` 객체 생성
- 헤더 변수 500+개 로드
- 표본: DIMSCALE, LTSCALE, LUNITS 등

#### [5-2] Classes 섹션
- 커스텀 클래스 정의 목록 생성
- 클래스 수 확인
- 클래스 이름, 번호 표시

#### [5-3] Handles 섹션
- `HandleRegistry` 생성 (핸들 → 파일 오프셋 매핑)
- 전체 핸들 수 확인

### STAGE 6: 전체 문서 파싱 (고수준 API)
- `DwgReader.open()` 호출로 위 모든 단계 자동 수행
- 최종 `DwgDocument` 객체 반환
- 결과 통계:
  - 전체 객체 수
  - 레이어 수 및 목록
  - 엔티티 수 및 타입 분포

---

## 사용법

### 1. 샘플 DWG 파일 준비
실제 AutoCAD DWG 파일을 준비하세요.

#### AutoCAD에서 생성하는 방법:
1. AutoCAD 실행
2. 새 드로잉 생성
3. 몇 가지 도형 추가 (예: Line, Circle, Layer)
4. 저장 (버전: R2004, R2007, R2013 등)

#### 버전별 추천 샘플:
- **R2004 (.dwg)**: 가장 호환성 높음 (Phase 2 구현)
- **R2007 (.dwg)**: LZ77 압축 테스트 (Phase 2 구현)
- **R13/R14 (.dwg)**: Phase 4 구현 (현재 미완성)

### 2. 테스트 실행

#### Windows:
```bash
run_test.bat path\to\sample.dwg
```

#### macOS/Linux:
```bash
./run_test.sh path/to/sample.dwg
```

#### 직접 Java 실행:
```bash
mvn compile
java -cp target/classes:target/dependency/* \
  io.dwg.test.DwgParsingStageTest path/to/sample.dwg
```

### 3. 테스트 출력 해석

#### 성공 예:
```
═══════════════════════════════════════════════════════════════
  DWG 파일 파싱 단계별 테스트
═══════════════════════════════════════════════════════════════
대상 파일: sample.dwg

[✓] STAGE 1: 파일 읽기 성공 (123456 bytes)

─────────────────────────────────────────────────────────────
STAGE 2: 버전 감지
─────────────────────────────────────────────────────────────
  ✓ 감지된 버전: R2007 (AC1021)

─────────────────────────────────────────────────────────────
STAGE 3: 헤더 파싱
─────────────────────────────────────────────────────────────
  ✓ 헤더 파싱 성공
    - 버전: R2007
    - 유지보수 버전: 0
    - 코드페이지: 20127
    - 보안 플래그: 0x00000000 (암호화 안됨)

[... 계속 ...]

═══════════════════════════════════════════════════════════════
  [✓] 모든 단계 파싱 완료
═══════════════════════════════════════════════════════════════
```

#### 실패 예:
```
─────────────────────────────────────────────────────────────
STAGE 5: 개별 섹션 파싱
─────────────────────────────────────────────────────────────
  [5-1] Header 섹션
    ✓ 파싱 성공
      - DIMSCALE: 1.00
      - LTSCALE: 1.00
      - LUNITS: 2
  [5-2] Classes 섹션
    ✗ 파싱 실패: IndexOutOfBoundsException
  [5-3] Handles 섹션
    ✓ 파싱 성공
      - 핸들 수: 145

[... 계속 ...]

═══════════════════════════════════════════════════════════════
  [✗] 파싱 실패
═══════════════════════════════════════════════════════════════
오류: IndexOutOfBoundsException
    at io.dwg.sections.classes.ClassesSectionParser.parse(...)
    ...
```

### 4. 실패 분석

실패 시 다음과 같이 대응합니다:

| 실패 단계 | 원인 추정 | 대응 |
|----------|---------|------|
| **STAGE 2** | 지원하지 않는 버전 | `DwgVersion`에 버전 추가, 핸들러 구현 |
| **STAGE 3** | 헤더 구조 차이 | 버전별 헤더 필드 재검토 |
| **STAGE 4** | 섹션 맵 파싱 오류 | 스펙 재확인, LZ77 압축 문제 |
| **STAGE 5-1** | 헤더 변수 필드 누락/타입 오류 | `HeaderSectionParser` 수정 |
| **STAGE 5-2** | 클래스 구조 파싱 오류 | Sentinel 또는 크기 계산 재검토 |
| **STAGE 5-3** | 핸들 맵 형식 차이 | Modular 인코딩 또는 블록 크기 재검토 |
| **STAGE 6** | Objects 섹션 파싱 | Entity 타입별 필드 추가, ObjectReader 구현 |

---

## 확장 가능성

### 새로운 엔티티 타입 추가:

1. `io.dwg.entities.concrete`에 새 클래스 생성:
```java
public class DwgPolyline extends AbstractDwgEntity {
    private List<Point3D> vertices;
    // ...
}
```

2. `ObjectTypeResolver`에 등록:
```java
resolver.register(new PolylineObjectReader());
```

3. `ObjectReader` 구현:
```java
public class PolylineObjectReader implements ObjectReader {
    @Override
    public void read(DwgObject target, BitStreamReader r, DwgVersion v) {
        // 파싱 로직
    }
}
```

4. 테스트 재실행하여 엔티티 수 확인

---

## 주요 테스트 체크리스트

- [ ] 샘플 DWG 파일 준비 (R2004 이상)
- [ ] STAGE 1-3 통과 (파일 읽기, 버전 감지, 헤더 파싱)
- [ ] STAGE 4 통과 (섹션 추출)
- [ ] STAGE 5 개별 섹션 파싱 성공
- [ ] STAGE 6 DwgDocument 생성 및 객체 조회 가능
- [ ] 엔티티별 필드 파싱 검증

---

## 디버깅 팁

### 1. 개별 단계 로깅 추가:
```java
// 테스트 코드에 다음 추가
BitStreamReader r = reader(stream, version);
System.out.printf("  [DEBUG] Position before: %d bits\n", r.position());
HeaderVariables vars = parse(stream, version);
System.out.printf("  [DEBUG] Position after: %d bits\n", r.position());
```

### 2. Hex Dump 확인:
```java
byte[] data = stream.rawBytes();
for (int i = 0; i < Math.min(256, data.length); i++) {
    System.out.printf("%02X ", data[i]);
    if ((i + 1) % 16 == 0) System.out.println();
}
```

### 3. 특정 섹션만 테스트:
```java
// 핸들 섹션만 파싱
HandleRegistry registry = new HandlesSectionParser()
    .parse(sections.get("AcDb:Handles"), version);
```

---

## 참고 자료

- OpenDesign Specification v5.4.1 (공개 스펙)
- DWG 파일 포맷: https://en.wikipedia.org/wiki/Autodesk_DWG
- AutoCAD 버전별 차이: Autodesk 공식 문서
