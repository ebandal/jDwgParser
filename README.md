# jDwgParser

Java로 구현한 AutoCAD DWG 파일 파서 라이브러리.

OpenDesign Specification v5.4.1을 기반으로 R13부터 R2018까지의 DWG 파일 읽기 및 쓰기를 지원합니다.

## 프로젝트 구조

```
src/
├── io/dwg/
│   ├── core/          # 저수준 I/O, 버전 감지, 유틸리티
│   │   ├── io/        # BitStream 읽기/쓰기
│   │   ├── type/      # DwgHandle, Point3D 등
│   │   ├── util/      # LZ77, CRC, 문자열 인코딩
│   │   └── version/   # DwgVersion, 버전 감지
│   │
│   ├── format/        # 버전별 파일 구조 핸들러
│   │   ├── common/    # 공통 인터페이스
│   │   ├── r13/       # R13-R15 구현
│   │   ├── r2004/     # R2004 구현
│   │   └── r2007/     # R2007-R2010 구현
│   │
│   ├── sections/      # 섹션 파서/라이터
│   │   ├── header/    # Header 변수
│   │   ├── classes/   # 클래스 정의
│   │   ├── handles/   # 핸들 맵
│   │   └── objects/   # 엔티티/객체
│   │
│   ├── entities/      # 도메인 모델 (순수 데이터)
│   │   ├── concrete/  # Line, Circle, Arc, Text 등
│   │   └── ...
│   │
│   ├── api/           # 고수준 공개 API
│   │   ├── DwgReader
│   │   ├── DwgWriter
│   │   └── DwgDocument
│   │
│   └── test/          # 테스트
│       └── DwgParsingStageTest
```

## 아키텍처

### 모듈 계층도

```
dwg-core → dwg-format → dwg-sections → dwg-entities → dwg-api
```

| 모듈 | 책임 |
|------|------|
| `dwg-core` | 비트스트림 I/O, 타입, LZ77/CRC |
| `dwg-format` | 버전별 파일 구조 (R13~R2018) |
| `dwg-sections` | 섹션 파서/라이터 |
| `dwg-entities` | 순수 데이터 모델 (엔티티+비엔티티) |
| `dwg-api` | 공개 API (DwgReader, DwgWriter) |

### 주요 패턴

- **Strategy**: `DwgFileStructureHandler` — 버전별 파싱 전략
- **Factory**: `DwgFileStructureHandlerFactory` — 버전에 맞는 핸들러 선택
- **Registry**: `SectionParserRegistry` — 섹션 이름 → 파서 매핑
- **Builder**: `DwgReader`, `DwgDocument` — Fluent API

## 구현 단계

### Phase 2 ✓ (현재 구현)

**dwg-core** (완료)
- BitStream I/O (읽기/쓰기)
- DWG 타입 (BitShort, BitLong, BitDouble 등)
- 버전 감지
- LZ77 압축/해제
- CRC 검증

**dwg-format** (부분 완료)
- R2004 헤더 파싱 및 섹션 추출 ✓
- R2007 헤더 파싱, 페이지 맵, 섹션 맵 ✓
- R13 헤더 파싱 ✓

**dwg-sections** (완료)
- Header 섹션 파서 (500+개 변수) ✓
- Classes 섹션 파서 ✓
- Handles 섹션 파서 ✓
- Objects 섹션 파서 (기본 엔티티) ✓

**dwg-entities** (완료)
- DwgObject, DwgEntity 인터페이스
- AbstractDwgObject, AbstractDwgEntity
- 기본 엔티티: Line, Circle, Arc, Text, Insert, Layer

**dwg-api** (완료)
- `DwgReader` — 파일 읽기 진입점
- `DwgDocument` — 파싱된 문서 표현
- `DwgWriter` — 뼈대 (Phase 3)

### Phase 3 (계획)

- R2004/R2007 쓰기 (헤더, 섹션 직렬화)
- 추가 엔티티 (Polyline, Hatch, MText 등)
- 메모리 최적화

### Phase 4 (계획)

- R13/R14 전체 구현
- 보조 섹션 파서 (AppInfo, Preview, XData 등)
- 전체 테스트 스위트

## 빌드 및 실행

### 요구사항

- Java 16+
- Maven 3.6+

### 빌드

```bash
mvn clean compile
```

### 테스트 (단계별 파싱 테스트)

```bash
# Windows
run_test.bat path/to/sample.dwg

# macOS/Linux
./run_test.sh path/to/sample.dwg

# 직접 실행
mvn compile
java -cp target/classes:target/dependency/* \
  io.dwg.test.DwgParsingStageTest path/to/sample.dwg
```

자세한 테스트 가이드는 [TESTING.md](TESTING.md) 참조.

## 사용 예시

### 기본 사용법

```java
import io.dwg.api.DwgReader;
import io.dwg.api.DwgDocument;
import io.dwg.entities.DwgEntity;
import java.nio.file.Paths;

// DWG 파일 읽기
DwgDocument doc = DwgReader.defaultReader()
    .open(Paths.get("drawing.dwg"));

// 문서 정보
System.out.println("버전: " + doc.version());
System.out.println("객체 수: " + doc.objectMap().size());

// 레이어 조회
doc.layer("0")
    .ifPresent(layer -> System.out.println("0 레이어 색상: " + layer.color()));

// 엔티티 조회
List<DwgEntity> entities = doc.entities();
for (DwgEntity e : entities) {
    System.out.println(e.objectType() + " at handle 0x" + Long.toHexString(e.handle()));
}
```

## 구현 상태

| 버전 | 읽기 | 쓰기 | 주요 엔티티 |
|------|------|------|-----------|
| R13  | 🟡 부분 | ❌ | - |
| R14  | 🟡 부분 | ❌ | - |
| R2000 | 🟡 부분 | ❌ | - |
| **R2004** | 🟢 완성 | 🟡 Phase 3 | Line, Circle, Arc, Text, Insert, Layer |
| **R2007** | 🟢 완성 | 🟡 Phase 3 | Line, Circle, Arc, Text, Insert, Layer |
| R2010 | 🟢 완성 | 🟡 Phase 3 | Line, Circle, Arc, Text, Insert, Layer |
| R2013 | 🟡 부분 | ❌ | - |
| R2018 | 🟡 부분 | ❌ | - |

🟢 완성 / 🟡 부분 / ❌ 미구현

## 단계별 파싱 흐름

```
[파일 읽기] → [버전 감지] → [헤더 파싱] → [섹션 추출] 
    ↓              ↓               ↓            ↓
 1단계         2단계          3단계        4단계

[섹션 파싱] → [객체 생성] → [DwgDocument] → [API 사용]
    ↓              ↓            ↓            ↓
  5단계         5단계        6단계        최종
```

각 단계는 독립적으로 테스트 가능합니다 ([DwgParsingStageTest](src/io/dwg/test/DwgParsingStageTest.java) 참고).

## 주요 클래스

### API 계층

- `DwgReader` — 파일 읽기
- `DwgWriter` — 파일 쓰기
- `DwgDocument` — 파싱된 문서

### 핵심 도메인

- `DwgObject` — 모든 객체의 기본 인터페이스
- `DwgEntity` — 도형 엔티티
- `DwgVersion` — 버전 열거 및 버전 비교
- `HeaderVariables` — 헤더 변수 (500+개)
- `HandleRegistry` — 핸들 → 오프셋 매핑

### 저수준 I/O

- `BitStreamReader` — 비트 단위 읽기
- `BitStreamWriter` — 비트 단위 쓰기
- `ByteBufferBitInput` — 버퍼 기반 입력
- `ByteBufferBitOutput` — 버퍼 기반 출력

## 라이선스

MIT License

## 참고 자료

- [OpenDesign Specification](https://www.opendesign.com/guestdownloads) — 공식 스펙
- AutoCAD DWG 포맷 위키
- Autodesk 공식 문서

## 기여

이슈 및 PR을 환영합니다.

## 개발 팀

- Initial design: ebandal
- Contributors: 커뮤니티 (현재)
