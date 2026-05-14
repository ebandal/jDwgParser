# Phase 7 & 8 Execution Guide

**Current Date**: 2026-04-28
**Status**: Phase 8 Tier 1 Complete, Ready for Phase 7 Validation
**Target**: Increase entity coverage from 55→74 types, 4→3000+ entities from R2007 files

## Quick Reference

| Phase | Status | Action | Timeline |
|-------|--------|--------|----------|
| 6C | ✅ COMPLETE | blockCount fix applied | Done (Apr 27-28) |
| 7 | 🔄 READY | Validate blockCount fix | TODAY (next session) |
| 8 Tier 1 | ✅ COMPLETE | IMAGE, WIPEOUT readers added | Done (Apr 28) |
| 8 Tier 2 | ⏳ PENDING | Implement ACAD_TABLE, XREF, etc | After Phase 7 ✅ |
| 8 Tier 3 | ⏳ OPTIONAL | Implement PROXY, FIELD, etc | If time permits |

---

## Phase 7: Validation Framework Execution

### Prerequisites
- Java compiler (✓ available: javac 21.0.6)
- Maven (✗ need to install: `mvn --version`)
- DWG test sample files (✓ in project: 141 files, 10 R2007 files)

### Installation Instructions (if needed)
```bash
# Install Maven (Windows)
choco install maven
# or download from https://maven.apache.org/download.cgi

# Verify
mvn --version
```

### Execution Steps

#### Step 1: Verify blockCount Fix is in Place
```bash
# Check R2007FileStructureHandler.java lines 165-170
# Should see: pesize = (compSize + 7) & ~7L; blockCount = (pesize + 250) / 251;
```

✅ **Status**: Fix verified in R2007FileStructureHandler

#### Step 2: Compile Project
```bash
cd c:/workspace_ebandal/jDwgParser
mvn clean compile
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: 30-60 seconds
```

**If errors occur**:
- Check Java version: `javac -version` (requires 16+)
- Check pom.xml: src directory should contain all source files
- Run `mvn clean` to clear old build artifacts

#### Step 3: Create Phase 7 Validation Test Suite

Create `src/test/java/phase7/ValidateHandlesFix.java`:
```bash
# See Phase 7 Test Code (below) - Create this file in src/test/java/phase7/
```

#### Step 4: Run Phase 7 Validation Tests

**Primary Test**:
```bash
mvn test -Dtest=ValidateHandlesFix
```

**Expected Output**:
- Entity count improvement: 4 → 900-2000+ (225-500x improvement)
- R2007 files with entities: 9-10 out of 10 files
- Offset validity: <5% invalid offsets for 8+ files
- Success rate: 80-100%

**Secondary Tests**:
```bash
mvn test -Dtest=TestHandlesOffsetQuality
mvn test -Dtest=IntegratedR2007Test
mvn test -Dtest=ValidateAllVersions141Files
```

**Expected Results**:

| Test | Success Criteria | Current | Expected |
|------|------------------|---------|----------|
| ValidateHandlesFix | Entity count | 4 | 900-2000+ |
| TestHandlesOffsetQuality | Invalid % | 57-95% | <5% |
| IntegratedR2007Test | Success rate | Low | 80%+ |
| Full Regression | R2000/R2004 | 604 entities | 604+ entities |

#### Step 5: Record Results

Create `PHASE_7_RESULTS.md` with:
1. Entity count by file
2. Offset validity percentages
3. Type distribution (which entity types appear)
4. Any failures or exceptions
5. Performance metrics (time to parse all 10 files)

**Template**:
```markdown
# Phase 7 Validation Results

## Summary
- Total entities parsed: XXX (was 4)
- Success rate: XX% (10/10 files)
- Invalid offsets: X% (expected <5%)

## Per-File Breakdown
| File | Entities | Valid% | Status |
|------|----------|--------|--------|
| Arc.dwg | XXX | YY% | ✅ |

## Entity Types Found
- Type 0x11 (ARC): XX instances
- Type 0x12 (CIRCLE): XX instances
- etc.

## Performance
- Parse time: X seconds
- Average per file: X ms

## Issues Found
(if any)
```

#### Step 6: Success Criteria Decision

**If Phase 7 SUCCEEDS** (entities >500):
→ Proceed to Phase 8 Tier 2 implementation immediately
→ Continue with: ACAD_TABLE, XREF, UNDERLAY, SURFACE, MESH

**If Phase 7 PARTIAL** (entities 100-500):
→ Document findings
→ Investigate specific files
→ May need targeted fixes
→ Then proceed to Phase 8

**If Phase 7 FAILS** (entities <100):
→ STOP - Investigate blockCount fix
→ Check R2007FileStructureHandler changes
→ Verify fix is applied to all version handlers
→ May need to debug deinterleaving/LZ77 pipeline

---

## Phase 8: Entity Type Expansion (After Phase 7 Success)

### Tier 1: Easy Implementation (COMPLETE)
✅ IMAGE (0x51) - Added
✅ WIPEOUT (0x52) - Added
✅ OLE2FRAME (0x3E) - Already existed

**Result**: 55 → 57 entity types

### Tier 2: Medium Complexity (NEXT)

#### Entity Type: ACAD_TABLE

**Type Code**: TBD (need to find from Phase 7 results)
**Complexity**: HIGH - Complex nested structure
**Time Estimate**: 4-5 hours

**Implementation Steps**:
1. Create `DwgTable.java` entity class
   - Properties: rows, columns, cells, styles, borders
   - Cell structure: content, format, alignment
   
2. Create `TableObjectReader.java`
   - Read table dimensions
   - Read cell data (variable-length structure)
   - Parse formatting information
   
3. Register in ObjectTypeResolver
   - Add to defaultResolver()
   
4. Test
   - Should parse any TABLE entities from Phase 7 results
   - Verify row/column counts
   - Check cell content population

**References**:
- OpenDesign Spec §29 (ACAD_TABLE)
- LibreDWG: src/dwg.c decode_table()

#### Entity Type: XREF (External Reference)

**Type Code**: TBD
**Complexity**: MEDIUM
**Time Estimate**: 2 hours

**Implementation Steps**:
1. Create `DwgXref.java` entity class
   - Properties: externalRef, insertionPoint, scale, rotation
   
2. Create `XrefObjectReader.java`
   - Read reference path
   - Read insertion point, scale
   - Read handling flags

#### Entity Type: UNDERLAY (PDF/DWF Underlays)

**Type Code**: TBD
**Complexity**: MEDIUM
**Time Estimate**: 2 hours

**Implementation Steps**:
1. Create `DwgUnderlay.java` entity class
2. Create `UnderlayObjectReader.java`
   - Read file path
   - Read extents and scale
   - Read transparency settings

#### Entity Type: SURFACE (NURBS Surface)

**Type Code**: TBD
**Complexity**: MEDIUM-HIGH
**Time Estimate**: 3 hours

**Implementation Steps**:
1. Create `DwgSurface.java` entity class
   - Properties: controlPoints, knots, weights, degree
   
2. Create `SurfaceObjectReader.java`
   - Read NURBS surface data
   - Parse control point grid
   - Handle knot vector

#### Entity Type: MESH (Free-Form Mesh)

**Type Code**: TBD
**Complexity**: MEDIUM
**Time Estimate**: 2 hours

**Implementation Steps**:
1. Create `DwgMesh.java` entity class
2. Create `MeshObjectReader.java`

**Expected Result after Tier 2**: 57 → 63 entity types

### Tier 3: Complex Implementation (OPTIONAL)

These are custom/proxy types that may not appear frequently:

#### Entity Types:
- ACAD_PROXY_ENTITY - Custom proxy entities
- ACAD_FIELD - Computed field values
- ACAD_DICTIONARY_VAR - Dictionary variables  
- SCALE - Scale objects
- VISUALSTYLE - Visual style objects

**Total Time**: 10-15 hours
**Expected Result**: 63 → 68+ entity types

---

## Workflow Recommendation

### Day 1: Phase 7 Validation
1. Morning: Ensure Maven is installed
2. Run ValidateHandlesFix and record results
3. Document findings
4. Decision: SUCCESS → proceed to Phase 8 | FAIL → debug

### Days 2-3: Phase 8 Tier 2 Implementation (if Phase 7 succeeds)
1. Morning: Create TableObjectReader (4-5 hours)
2. Afternoon: Create XrefObjectReader + UnderlayObjectReader (4 hours)
3. Evening: Create SurfaceObjectReader + MeshObjectReader (5 hours)
4. Regression test on all 141 files

### Days 4-5: Phase 8 Tier 3 Implementation (if time permits)
1. Implement proxy entity support
2. Implement field entity support
3. Full regression testing

---

## Critical Path to Success

```
Phase 6C (Fixed) 
  ↓
Phase 7 Validation ← YOU ARE HERE
  ↓ (if SUCCESS)
Phase 8 Tier 1 (DONE: IMAGE, WIPEOUT)
  ↓
Phase 8 Tier 2 (NEXT: TABLE, XREF, UNDERLAY)
  ↓
Phase 8 Tier 3 (OPTIONAL: PROXY, FIELD)
  ↓
Phase 9 (Advanced: Blocks, XDATA, References)
  ↓
Phase 10 (Release: API, Packaging, Docs)
```

## Debugging Guide

### If Phase 7 Shows Low Entity Counts (<100)

**Step 1**: Check blockCount fix applied
```bash
grep -A2 "pesize.*7" src/io/dwg/format/R2007FileStructureHandler.java
```

**Step 2**: Verify fix affects all version handlers
- R2007FileStructureHandler ✓
- R2010FileStructureHandler (should inherit)
- R2013FileStructureHandler (should inherit)
- R2018FileStructureHandler (should inherit)

**Step 3**: Debug specific file
```bash
# Create simple test
java -cp src io.dwg.test.R2007HandlesDebugTest Arc.dwg
```

### If IMAGE/WIPEOUT Readers Don't Parse

**Check**:
1. Type codes match actual file data
   - Use hex dump tool to find actual type codes in Objects section
   - Compare with DwgObjectType enum
   
2. BitStreamReader methods correct
   - Verify read3BitDouble() method exists
   - Verify readBitShort() exists
   
3. Reader registered correctly
   - Check ObjectTypeResolver.defaultResolver()
   - Verify ImageObjectReader and WipeoutObjectReader added

---

## Code Quality Checklist

Before considering a type "COMPLETE":

- [ ] Entity class created (extends AbstractDwgEntity)
- [ ] Reader class created (implements ObjectReader)
- [ ] Reader registered in ObjectTypeResolver
- [ ] All required methods implemented
- [ ] BitStreamReader usage correct
- [ ] No compilation errors (`mvn compile`)
- [ ] Tested with sample files (mvn test)
- [ ] Regression test passes (mvn test -Dtest=*141Files)

---

## Expected Final Metrics

### After Phase 7 (if successful):
```
Entity count:        4 → 900-2000+
R2007 file coverage: 1/10 → 9-10/10 files
Invalid offsets:     57-95% → <5%
```

### After Phase 8 Tier 1 (complete):
```
Entity types:   55 → 57 types
Coverage:       55% → 57% of AutoCAD types
```

### After Phase 8 Tier 2 (target):
```
Entity types:   57 → 63 types
Coverage:       63% → 63% of AutoCAD types
Entity count:   2500+ → 3000+
```

### After Phase 8 Tier 3 (if completed):
```
Entity types:   63 → 68+ types
Coverage:       68+ % of AutoCAD types
Entity count:   3000+ → 3500+
```

---

## References

- **Phase 6C Completion**: SESSION_2026_05_05_PHASE_8_START.md
- **Validation Plan**: PHASE_7_PLAN.md
- **Detailed Roadmap**: PHASE_8_DETAILED_PLAN.md
- **Current Progress**: PHASE_8_PROGRESS.md
- **Entity Analysis**: ENTITY_TYPE_ANALYSIS.md
- **Specification**: doc/OpenDesign_Specification_for_.dwg_files.pdf
- **Reference**: C:\workspace_ebandal\libredwg

---

## Key Files

### Phase 6C Changes
- `src/io/dwg/format/R2007FileStructureHandler.java` - blockCount fix (lines 165-170)

### Phase 8 Tier 1 (Completed)
- `src/io/dwg/entities/concrete/DwgImage.java` - NEW
- `src/io/dwg/sections/objects/readers/ImageObjectReader.java` - NEW
- `src/io/dwg/entities/concrete/DwgWipeout.java` - NEW
- `src/io/dwg/sections/objects/readers/WipeoutObjectReader.java` - NEW
- `src/io/dwg/entities/DwgObjectType.java` - MODIFIED (added IMAGE, WIPEOUT)
- `src/io/dwg/sections/objects/ObjectTypeResolver.java` - MODIFIED (registered readers)

### Phase 8 Tier 2 (To be created)
- `src/io/dwg/entities/concrete/DwgTable.java` - TBD
- `src/io/dwg/sections/objects/readers/TableObjectReader.java` - TBD
- (similar for XREF, UNDERLAY, SURFACE, MESH)

---

**Last Updated**: 2026-04-28
**Next Action**: Execute Phase 7 Validation Tests
