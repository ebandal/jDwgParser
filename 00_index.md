# DWG 파서 라이브러리 — 클래스 명세 인덱스

Java DWG 파서 라이브러리 전체 클래스 명세 + 상세 설계.
스펙: Open Design Specification for .dwg files v5.4.1

## 문서 목록

| 파일 | 모듈 | 클래스 수 | 내용 |
|---|---|---|---|
| [01_dwg-core.md](01_dwg-core.md) | `dwg-core` | 35개 | BitStream I/O, 타입 시스템, 버전 감지, LZ77, CRC, 예외 |
| [02_dwg-format.md](02_dwg-format.md) | `dwg-format` | 28개 | R13/R14/R2004/R2007/R2013/R2018 파일 구조 핸들러 |
| [03_dwg-sections.md](03_dwg-sections.md) | `dwg-sections` | 38개 | 섹션 파서/라이터 (§9~§28 전체) |
| [04_dwg-entities.md](04_dwg-entities.md) | `dwg-entities` | 82개 | 도메인 객체 모델 (§20 전체 엔티티) |
| [05_dwg-api-and-test.md](05_dwg-api-and-test.md) | `dwg-api` + `dwg-test` | 40개 | Public API, 테스트, JMH 벤치마크 |

**총계: 223개 클래스 / 인터페이스 / enum / record**

각 파일은 **클래스 명세(High-level)** + **상세 설계(Detailed)** 두 섹션으로 구성된다.
상세 설계 섹션은 `# 상세 설계 보충` 헤더로 시작한다.

## 표기 규칙

- `*(interface)*` — 인터페이스
- `*(abstract)*` — 추상 클래스
- `*(enum)*` — 열거형
- `*(record)*` — Java 16+ record

## 비트 타입 약어 (§2 기준)

| 약어 | 풀이 | 크기 |
|---|---|---|
| `B` | Bit | 1 bit |
| `BB` | 2-Bit | 2 bits |
| `RC` | Raw Char | 1 byte |
| `RS` | Raw Short | 2 bytes LE |
| `RL` | Raw Long | 4 bytes LE |
| `RD` | Raw Double | 8 bytes LE IEEE754 |
| `BS` | Bit Short | 가변 (2-bit opcode) |
| `BL` | Bit Long | 가변 (2-bit opcode) |
| `BLL` | Bit Long Long | 가변 (3-bit byte count) |
| `BD` | Bit Double | 가변 (2-bit opcode) |
| `BDWMD` | Bit Double With Default | 가변 (2-bit opcode + default) |
| `MC` | Modular Char | 가변 (MSB=continue, bit6=sign) |
| `MS` | Modular Short | 가변 (2바이트 단위) |
| `T` | Text | BS길이 + RC[] |
| `TU` | Unicode Text | BS 문자수 + UTF-16LE |
| `TV` | Variable Text | R2007+: TU, 미만: T |
| `H` | Handle Reference | 4bit code + 4bit count + bytes |
| `BE` | Bit Extrusion | R2000+: 1bit (0,0,1이면 true); R13/14: 3BD |
| `BT` | Bit Thickness | R2000+: 1bit (0.0이면 true); R13/14: BD |
| `DD` | Bit Double With Default | BD의 default 기반 최적화 변형 |
| `2RD` | 2D Raw Double | RD + RD |
| `3RD` | 3D Raw Double | RD + RD + RD |
| `2BD` | 2D Bit Double | BD + BD |
| `3BD` | 3D Bit Double | BD + BD + BD |

## 구현 우선순위

```
Phase 1 (필수)     dwg-core 전체 → BitStream 완성
Phase 2 (MVP)      dwg-format(R2004) + dwg-sections(Header/Classes/Handles/Objects)
                   + dwg-entities(Line/Circle/Arc/Text/Insert/Layer) + dwg-api(Reader)
Phase 3 (확장)     dwg-format(R2007/R2013) + dwg-api(Writer) + 나머지 엔티티
Phase 4 (완성)     dwg-format(R13/R14) + 보조섹션 전부 + dwg-test 전부
```

## 모듈 간 의존 관계

```
dwg-core          (no dependencies)
    ↑
dwg-format        (depends on: dwg-core)
    ↑
dwg-sections      (depends on: dwg-core, dwg-entities)
    ↑
dwg-entities      (depends on: dwg-core only)
    ↑
dwg-api           (depends on: all modules)
dwg-test          (depends on: all modules)
```

## 스펙 섹션 → 구현 모듈 매핑

| 스펙 섹션 | 내용 | 구현 위치 |
|---|---|---|
| §2 | Bit codes and data definitions | `dwg-core` BitStreamReader/Writer |
| §3 | R13-R15 file format | `dwg-format` R13FileStructureHandler |
| §4 | R2004 file format | `dwg-format` R2004FileStructureHandler |
| §5 | R2007 file format | `dwg-format` R2007FileStructureHandler |
| §7 | R2013 file format | `dwg-format` R2013FileStructureHandler |
| §8 | R2018 file format | `dwg-format` R2018FileStructureHandler |
| §9 | Header Variables | `dwg-sections` HeaderSectionParser |
| §10 | Classes | `dwg-sections` ClassesSectionParser |
| §13 | SummaryInfo | `dwg-sections` SummaryInfoParser |
| §14 | Preview | `dwg-sections` PreviewSectionParser |
| §20 | Object types (all) | `dwg-sections` ObjectsSectionParser + `dwg-entities` 전체 |
| §23 | Object Map (Handles) | `dwg-sections` HandlesSectionParser |
| §27 | Auxiliary Header | `dwg-sections` AuxHeaderParser |
| §28 | Extended Entity Data | `dwg-sections` XDataParser |
| §29 | Proxy Entity Graphics | `dwg-sections` ProxyEntityParser |
