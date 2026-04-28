# Entity Type Coverage Analysis - Phase 8 Roadmap

## Current Status

### Implemented Readers (55 types)

**Basic Geometry** (11):
- Line, Circle, Arc, Ellipse (4) ✅
- Polyline, LWPolyline, Polyline3D, Trace (4) ✅
- Spline (1) ✅
- Solid, Solid3D (2) ✅

**Text & Annotations** (4):
- Text, MText (2) ✅
- Attrib, Attdef (2) ✅

**Dimensions** (8):
- Dimension[Aligned, Ang3pt, Diameter, Linear, Radius] (5) ✅
- Tolerance (1) ✅
- Leader (1) ✅
- MLineStyle (1) ✅

**Insertions** (2):
- Insert, Minsert (2) ✅

**Lines & References** (3):
- Ray, XLine, MLine (3) ✅

**Mesh & Surface** (4):
- Face3D, PolylineMesh, Vertex[2D, 3D, Mesh, Pface] (6) ✅
- Region, Body (2) ✅

**Administrative Objects** (13):
- Block[Header, End], Layer, Ltype, Style, Ucs, View, Viewport, Vport (8) ✅
- AppId, DimStyle, Group, Dictionary, Layout, SeqEnd, Shape (7) ✅

**Total: 55 types implemented**

---

## Missing/Incomplete Types (High Priority)

### Tier 1: Easy to Implement (30-60 min each)

| Type | Status | Complexity | Notes |
|------|--------|-----------|-------|
| **WIPEOUT** | Not implemented | Easy | Raster image display |
| **IMAGE** | Not implemented | Easy | Embedded image |
| **OLE2FRAME** | Not implemented | Easy | OLE object reference |
| **MTEXT (variants)** | Partial | Easy | Enhanced MTEXT features |
| **LWPOLYLINE (3D)** | Partial | Easy | 3D variant support |
| **ARC (variants)** | Partial | Easy | Arc with special flags |

**Estimated: 6-8 types, 3-4 hours**

### Tier 2: Medium Complexity (1-2 hours each)

| Type | Status | Complexity | Notes |
|------|--------|-----------|-------|
| **ACAD_TABLE** | Not implemented | Medium | Complex table structure |
| **BLOCK (full)** | Partial | Medium | Block with extensions |
| **XREF** | Not implemented | Medium | External references |
| **UNDERLAY** | Not implemented | Medium | PDF/DWF underlays |
| **SURFACE** | Not implemented | Medium | NURBS surfaces |
| **MESH** | Not implemented | Medium | Free-form mesh |

**Estimated: 6 types, 8-10 hours**

### Tier 3: Complex (2-4 hours each)

| Type | Status | Complexity | Notes |
|------|--------|-----------|-------|
| **ACAD_PROXY_*** | Not implemented | Complex | Custom proxy entities |
| **ACAD_FIELD** | Not implemented | Complex | Computed field values |
| **ACAD_DICTIONARY_VAR** | Not implemented | Complex | Dict variables |
| **SCALE** | Not implemented | Medium-Complex | Scale objects |
| **VISUALSTYLE** | Not implemented | Medium-Complex | Visual style object |

**Estimated: 5 types, 10-15 hours**

---

## Implementation Strategy

### Phase 8 Priority Order

**Week 1: Tier 1 (Easy, high ROI)**
```
Day 1: IMAGE, WIPEOUT, OLE2FRAME (3 hours)
Day 2: MTEXT variants, Arc variants (2 hours)
Day 3: LWPolyline 3D support (1 hour)
Total: 6 hours, +6-8 entity types
```

**Week 2: Tier 2 (Medium, good ROI)**
```
Day 4-5: ACAD_TABLE (4 hours) - Most complex
Day 6: XREF, UNDERLAY (3 hours)
Day 7: SURFACE, MESH (3 hours)
Total: 10 hours, +6 types
```

**Week 3: Tier 3 (Complex, optional)**
```
Day 8-9: ACAD_PROXY_ENTITY (4 hours)
Day 10: ACAD_FIELD, misc (3 hours)
Total: 7 hours, +3-5 types
```

**Expected Result**: 55 + 15-19 = **70-74 entity types**

---

## Quick Wins (Start Today)

### 1. Add IMAGE Reader (30 min)

```java
public class ImageObjectReader extends ObjectReader<ImageEntity> {
    @Override
    public ImageEntity read(BitStreamReader reader) throws Exception {
        CommonObjectData common = readCommonHeader(reader);
        
        // IMAGE-specific fields
        DwgVector origin = reader.readVector3D();
        DwgVector xAxis = reader.readVector3D();
        DwgVector yAxis = reader.readVector3D();
        
        double width = reader.readDouble();
        double height = reader.readDouble();
        
        // Image properties
        byte[] imageData = reader.readRawBytes(...);
        String imagePath = reader.readUCS2String();
        
        ImageEntity entity = new ImageEntity(common);
        entity.setPosition(origin);
        entity.setWidth(width);
        entity.setHeight(height);
        entity.setImagePath(imagePath);
        
        return entity;
    }
}
```

**Files to modify**:
- Create: `ImageObjectReader.java`
- Update: `ObjectReaderRegistry.java` (add registration)
- Update: `ObjectTypeResolver.java` (add type code 0x1F9)

### 2. Add WIPEOUT Reader (30 min)

Similar to IMAGE but simpler - just rectangular area with properties.

### 3. Add OLE2FRAME Reader (30 min)

Reference to OLE object - minimal parsing needed.

---

## Code Locations

### Reader Template
**File**: `src/decode/reader/LineObjectReader.java`
```java
public class LineObjectReader extends ObjectReader<LineEntity> {
    @Override
    public LineEntity read(BitStreamReader reader) throws Exception {
        CommonObjectData common = readCommonHeader(reader);
        
        DwgPoint start = reader.readPoint3D();
        DwgPoint end = reader.readPoint3D();
        
        LineEntity entity = new LineEntity(common);
        entity.setStartPoint(start);
        entity.setEndPoint(end);
        
        return entity;
    }
}
```

### Type Code Mapping
**File**: `src/io/dwg/sections/objects/ObjectTypeResolver.java`
```java
// Add to getEntityType() or similar method:
case 0x1F9: return DwgObjectType.IMAGE;
case 0x1FA: return DwgObjectType.WIPEOUT;
case 0x1FB: return DwgObjectType.OLE2FRAME;
```

### Reader Registration
**File**: `src/decode/reader/ObjectReaderRegistry.java`
```java
public void registerDefaultReaders() {
    // Existing readers...
    
    // Add Phase 8 readers:
    registry.put(DwgObjectType.IMAGE, new ImageObjectReader());
    registry.put(DwgObjectType.WIPEOUT, new WipeoutObjectReader());
    registry.put(DwgObjectType.OLE2FRAME, new Ole2FrameObjectReader());
}
```

---

## Test Plan

### For Each New Reader
```bash
# 1. Add test file with entity
# 2. Run ValidateAllVersions141Files
# 3. Verify entity is parsed (not counted as failure)
# 4. Check entity properties are correct
```

### Metrics
- Entity count: Should increase by ~X per new type
- Success rate: Should improve towards 100%
- No regressions: R2000/R2004 counts unchanged

---

## Expected Phase 8 Results

### Conservative Estimate
- **New types added**: 10-12
- **Total types**: 65-67
- **New entities**: +200-300
- **Total entities**: 2800-3000

### Optimistic Estimate  
- **New types added**: 15-19
- **Total types**: 70-74
- **New entities**: +500-800
- **Total entities**: 3100-3400

### Coverage Improvement
- **Before Phase 8**: 55/100 = 55%
- **After Phase 8**: 70-74/100 = 70-74%
- **Gap to 90%**: Still need 16-20 types

---

## Decision: Start with Easy Wins

**Recommendation**: Implement Tier 1 types first
- **Effort**: 6 hours
- **Expected improvement**: +200-300 entities
- **Risk**: Very low
- **ROI**: High

**Next action**: Start with IMAGE reader today

---

## References

- **Entity specs**: OpenDesign Specification §20-47
- **Type codes**: ObjectTypeResolver.java
- **Reader pattern**: LineObjectReader.java
- **Registry**: ObjectReaderRegistry.java
