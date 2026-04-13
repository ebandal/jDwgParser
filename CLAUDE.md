# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Java DWG parser library** specification project. It targets parsing/writing AutoCAD DWG files per the OpenDesign Specification v5.4.1 (see `doc/OpenDesign_Specification_for_.dwg_files.pdf`). All 223 planned classes are documented in spec files; no source code exists yet.

## Build & Test Commands

No build files exist yet. When implementing, the project will use:
- **Build tool:** Maven or Gradle (TBD)
- **Java version:** 16+ (uses Java Records)
- **Test framework:** JUnit 5 + Mockito
- **Benchmarks:** JMH (Java Microbenchmark Harness)

## Module Architecture

The library is split into 5 modules with strict layering (no upward dependencies):

```
dwg-core → dwg-format → dwg-sections → dwg-entities → dwg-api
```

| Module | Spec file | Classes | Responsibility |
|---|---|---|---|
| `dwg-core` | `01_dwg-core.md` | 35 | Bit-level I/O, types, LZ77, CRC, exceptions |
| `dwg-format` | `02_dwg-format.md` | 28 | Version-specific file structure handlers (R13–R2018) |
| `dwg-sections` | `03_dwg-sections.md` | 38 | Section parsers/writers (§9–§28) |
| `dwg-entities` | `04_dwg-entities.md` | 82 | Pure domain model (no parsing logic) |
| `dwg-api` + `dwg-test` | `05_dwg-api-and-test.md` | 40 | Public API, query builder, test infra, JMH benchmarks |

## Implementation Phases

```
Phase 1  dwg-core — BitStream I/O, version detection, LZ77/CRC utilities
Phase 2  dwg-format(R2004) + dwg-sections(Header/Classes/Handles/Objects)
         + dwg-entities(Line/Circle/Arc/Text/Insert/Layer) + dwg-api(Reader only)
Phase 3  dwg-format(R2007/R2013) + dwg-api(Writer) + remaining entities
Phase 4  dwg-format(R13/R14) + all auxiliary sections + full test suite
```

## Key Architectural Patterns

- **Strategy** — `DwgFileStructureHandler` selects parsing logic per DWG version
- **Factory** — `DwgFileStructureHandlerFactory` and `ObjectTypeResolver` dispatch by version/type
- **Builder** — `DwgReader`, `DwgWriter`, `DwgDocumentBuilder`, `DwgQueryBuilder` (fluent API)
- **Registry** — `SectionParserRegistry` dispatches section names to parser implementations
- **Two-phase object resolution** — First build handle→offset map, then resolve object graph references

## Critical Implementation Notes

- **R2007+ complexity:** File structure adds LZ77 compression and encryption on top of section data — handle in `dwg-format` before passing to `dwg-sections`
- **Text encoding:** CodePage-based for R2000 and earlier; UTF-16LE for R2007+
- **Handle references** (`DwgHandle`, `DwgHandleRef`) are resolved lazily after the full object map is loaded
- **Java Records** are used for immutable value types (coordinates, events, config); requires JDK 16+
- **`dwg-entities` is pure data model** — no I/O or parsing logic belongs there
- **CRC validation:** CRC-8 for object data, CRC-32 for section integrity

## Spec Notation

In the `*.md` spec files:
- `*(interface)*` — interface
- `*(abstract)*` — abstract class
- `*(enum)*` — enum
- `*(record)*` — Java 16+ record
