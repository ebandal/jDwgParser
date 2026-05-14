# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Java DWG parser library** specification project. It targets parsing AutoCAD DWG files per the OpenDesign Specification v5.4.1 (see `doc/OpenDesign_Specification_for_.dwg_files.pdf`). All 223 planned classes are documented in spec files; no source code exists yet.

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
- **Java Records** are used for immutable value types (coordinates, events, config); requires JDK 16+
- **`dwg-entities` is pure data model** — no I/O or parsing logic belongs there
- **CRC validation:** CRC-8 for object data, CRC-32 for section integrity

## Spec Notation

In the `*.md` spec files:
- `*(interface)*` — interface
- `*(abstract)*` — abstract class
- `*(enum)*` — enum
- `*(record)*` — Java 16+ record

---

## Investigation Priority for Uncertain Matters

When implementation details are unclear, follow this **strict priority order**:

### 1️⃣ **Spec Document (PRIMARY)**
- **First source:** OpenDesign Specification PDF (`doc/OpenDesign_Specification_for_.dwg_files.pdf`)
- **Second source:** Project spec files (`01_dwg-core.md` through `05_dwg-api-and-test.md`)
- **Third source:** Version comparison matrix in `03_dwg-sections.md`
- **Action:** Read the relevant spec section (§ number), extract exact format/algorithm
- **Time investment:** 30-60 minutes per uncertainty
- **Outcome:** Authoritative understanding, document findings in project spec

### 2️⃣ **LibreDWG Source Code (SECONDARY)**
- **When:** Spec is ambiguous or incomplete
- **Where:** Local copy at `C:\workspace_ebandal\libredwg` (shared reference implementation)
- **How to use:**
  - Search for function handling the same structure (e.g., `decode_handles()`)
  - Read the implementation logic
  - Cross-reference with spec to understand intent
  - Document the insight in project memory
- **Time investment:** 20-40 minutes per lookup
- **Outcome:** Proven working implementation, reference for edge cases
- **Important:** DO NOT copy code directly; understand the logic and reimplement

### 3️⃣ **Reverse Engineering (TERTIARY - Last Resort)**
- **When:** Spec is silent AND LibreDWG has no equivalent code
- **How:**
  1. Create binary analysis tool (hex dump, marker search)
  2. Test on multiple sample files
  3. Document patterns in memory
  4. Verify against known-good data
- **Time investment:** 1-2 hours, may fail
- **Outcome:** Possible insight, often incomplete
- **Risk:** High uncertainty, prone to false patterns
- **Use sparingly:** Only when 1️⃣ and 2️⃣ are exhausted

---

## Decision Record Template

When investigating uncertain matters, document the decision:

```
## [Issue Name]

### Question
What is the [exact technical detail]?

### Investigation Path
1. ✅ Spec check: [Section X.Y mentions...]
2. ✅ LibreDWG: [function name shows...]
3. ❌ Reverse engineering: [not needed]

### Decision
[Final conclusion with confidence level]

### Reference
- Spec §X.Y: [quote]
- LibreDWG: [file:function]
- Memory: [document link]
```

---

## Key Examples from Phase 5 (R2000 Investigation)

### Example: "What is R2000 Objects Section Structure?"

**Correct approach (used):**
1. ✅ **Spec:** Read §3 (R13-R2000), §10 (Classes), §23 (Handles), §20 (Objects)
   - Found: "Group A (R13~R2002): Classes/Handles/Objects in one section"
   - Found: Handles use "RS_BE page_size" (big-endian!)
   
2. ✅ **LibreDWG:** Cross-check `decode_handles()`, `decode_objects()`
   - Confirmed: PageSize reads as RS_BE (most significant byte first)
   - Confirmed: Handle deltas use MC encoding
   
3. ❌ **Reverse engineering:** Not needed (Spec + LibreDWG sufficient)

**Result:** Clear implementation path with 100% confidence

### Anti-Pattern Example (Avoided)

**Wrong approach (what we avoided):**
1. ❌ **Reverse engineering first:** Binary analysis with `00 FF` marker search
   - Result: False patterns, wasted 2+ hours
   - Conclusion: "Structure unclear"
   
2. ❌ **Skip spec reading:** Too long, too abstract
   - Result: Missed critical detail (RS_BE byte order)
   
3. ❌ **Ignore LibreDWG:** Wrote own code from scratch
   - Result: Different algorithm, incompatible output

---

## When to Write Memory Documents

Use memory (`C:\Users\heesu\.claude\projects\*\memory\`) to record:
- ✅ Investigation findings (what Spec/LibreDWG revealed)
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
