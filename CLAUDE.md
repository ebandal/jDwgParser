# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Java DWG parser library** specification project. It targets parsing AutoCAD DWG files per the OpenDesign Specification v5.4.1 (see `doc/OpenDesign_Specification_for_.dwg_files.pdf`). All 223 planned classes are documented in spec files; no source code exists yet.

## Build & Test Commands

No build files exist yet. When implementing, the project will use:
- **Build tool:** Maven or Gradle (TBD)
- **Java version:** 11+ (the build targets release 11: no records, text blocks, arrow switches or instanceof patterns)
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
| `dwg-sections` | `03_dwg-sections.md` | 38 | Section parsers (§9–§28) |
| `dwg-entities` | `04_dwg-entities.md` | 82 | Pure domain model (no parsing logic) |
| `dwg-api` + `dwg-test` | `05_dwg-api-and-test.md` | 40 | Public API, query builder, test infra, JMH benchmarks |

## Implementation Phases

```
Phase 1  dwg-core — BitStream I/O, version detection, LZ77/CRC utilities
Phase 2  dwg-format(R2004) + dwg-sections(Header/Classes/Handles/Objects)
         + dwg-entities(Line/Circle/Arc/Text/Insert/Layer) + dwg-api(Reader only)
Phase 3  dwg-format(R2007/R2013) + remaining entities
Phase 4  dwg-format(R13/R14) + all auxiliary sections + full test suite
```

## Key Architectural Patterns

- **Strategy** — `DwgFileStructureHandler` selects parsing logic per DWG version
- **Factory** — `DwgFileStructureHandlerFactory` and `ObjectTypeResolver` dispatch by version/type
- **Builder** — `DwgReader`, `DwgDocumentBuilder`, `DwgQueryBuilder` (fluent API)
- **Registry** — `SectionParserRegistry` dispatches section names to parser implementations
- **Two-phase object resolution** — First build handle→offset map, then resolve object graph references

## Critical Implementation Notes

- **R2007+ complexity:** File structure adds LZ77 compression and encryption on top of section data — handle in `dwg-format` before passing to `dwg-sections`
- **Text encoding:** CodePage-based for R2000 and earlier; UTF-16LE for R2007+
- **Handle references** (`DwgHandle`, `DwgHandleRef`) are resolved lazily after the full object map is loaded
- **Immutable value types** (coordinates, issues, ...) are final classes with record-style accessors (`x()`), not Java records, to stay on Java 11
- **`dwg-entities` is pure data model** — no I/O or parsing logic belongs there
- **CRC validation:** CRC-8 for object data, CRC-32 for section integrity

## Spec Notation

In the `*.md` spec files:
- `*(interface)*` — interface
- `*(abstract)*` — abstract class
- `*(enum)*` — enum
- `*(record)*` — an immutable value class (written as a final class, Java 11)

---

## Investigation Priority for Uncertain Matters

This library is written **from the specification only** (clean-room), so that it can be offered under a
commercial license. Never read, copy or port the source code of any other DWG implementation
(open source or commercial), and do not cite one as a source.

When implementation details are unclear, follow this order:

### 1️⃣ **Spec Document (PRIMARY)**
- **Source:** OpenDesign Specification PDF (`doc/OpenDesign_Specification_for_.dwg_files.pdf`;
  `pdftotext -layout` gives searchable text)
- **Action:** Read the relevant section (§ number) and implement the format or algorithm from it
- **In code:** name the section in the comment, e.g. `// spec §20.4.85`

### 2️⃣ **Experiments on Sample Files (SECONDARY)**
- **When:** The spec is ambiguous, silent, or contradicts itself
- **How:**
  1. Write the possible readings as variants
  2. Run them over all sample files and measure which one decodes consistently
     (sections byte-identical, objects ending at their stated bit size, versions rendering alike)
  3. Keep the variant the files support, and say in the comment that the samples settled it
- **If no sample can settle it:** keep the most plausible reading, mark it "not yet checked against a
  file" in the comment, and add it to `TODO.md`

### 3️⃣ **Published Literature (for general algorithms)**
- Textbooks and papers may be used for algorithms that are not DWG-specific
  (e.g. Reed-Solomon decoding: Lin & Costello, *Error Control Coding*; Berlekamp-Massey, Chien, Forney)
- Implement from the description, not from someone else's code; cite the paper or book in the comment

---

## Decision Record Template

When investigating uncertain matters, document the decision:

```
## [Issue Name]

### Question
What is the [exact technical detail]?

### Investigation Path
1. Spec: [Section X.Y says...]
2. Samples: [variant A vs B, what was measured, result]

### Decision
[Final conclusion with confidence level]

### Reference
- Spec §X.Y: [quote]
- Measurement: [files, counts]
```

---

## Verifying a Rewrite

When replacing an existing implementation, keep the old one temporarily and compare both on all
sample files (byte for byte), fuzz the new one with random input, then delete the old copy.

---

## When to Write Memory Documents

Use memory (`C:\Users\heesu\.claude\projects\*\memory\`) to record:
- ✅ Investigation findings (what the spec and the sample files revealed)
- ✅ Design decisions (why we chose approach X over Y)
- ✅ Critical insights (edge cases, version differences)
- ❌ Do NOT record: day-to-day debugging, temporary analysis, dead ends

### Memory Hierarchy

1. **Investigation Results** (e.g., `PHASE5_R2000_SPEC_ANALYSIS.md`)
   - Permanent record of what was learned
   - Used to brief future sessions

2. **Decision Points** (e.g., `PHASE5_R2000_DEEP_ANALYSIS.md`)
   - Record the "why" not just the "what"
   - Prevents repeating same investigation

3. **Implementation Plans** (e.g., `PHASE5_TABLE_LOCATORS_COMPLETE.md`)
   - Architecture decisions
   - Test strategy
   - Known limitations
