# Auxiliary Table Section Parsers - API Documentation

## Overview

Phase 4 introduces three high-priority auxiliary table section parsers for extracting table-based information from DWG files:

- **LayerTableParser** - Extracts layer definitions
- **LinetypeTableParser** - Extracts linetype patterns
- **StyleTableParser** - Extracts text style definitions

All parsers are automatically registered in the `SectionParserRegistry` and support all DWG versions (R13-R2018).

## Usage

### Basic Usage

```java
// Parsers are auto-registered in the default registry
SectionParserRegistry registry = SectionParserRegistry.defaultRegistry();

// Layer parser
SectionParser<?> layerParser = registry.get("AcDb:Layers");
List<DwgLayer> layers = (List<DwgLayer>) layerParser.parse(section, DwgVersion.R2004);

// Linetype parser
SectionParser<?> ltParser = registry.get("AcDb:Linetypes");
List<Map<String, Object>> linetypes = (List<Map<String, Object>>) ltParser.parse(section, DwgVersion.R2004);

// Style parser
SectionParser<?> styleParser = registry.get("AcDb:Styles");
List<DwgStyle> styles = (List<DwgStyle>) styleParser.parse(section, DwgVersion.R2004);
```

### In a Pipeline

```java
// Parse DWG file
DwgFileStructureHandler handler = DwgFileStructureHandlerFactory.detect(fileBytes);
FileHeaderFields header = handler.readHeader(input);
Map<String, SectionInputStream> sections = handler.readSections(input, header);

// Extract layers
SectionInputStream layersSec = sections.get("AcDb:Layers");
if (layersSec != null) {
    LayerTableParser parser = new LayerTableParser();
    List<DwgLayer> layers = parser.parse(layersSec, handler.version());
    for (DwgLayer layer : layers) {
        System.out.println("Layer: " + layer.getName() + " (frozen=" + layer.isFrozen() + ")");
    }
}
```

## API Reference

### LayerTableParser

**Purpose**: Extracts layer definitions from table data.

**Section Name**: `AcDb:Layers`

**Return Type**: `List<DwgLayer>`

**Layer Properties**:
- `name` (String) - Layer name
- `frozen` (boolean) - Layer frozen in viewport
- `on` (boolean) - Layer visible
- `locked` (boolean) - Layer locked for editing
- `flags` (int) - Raw flags bitmap
- `color` (CmColor) - Layer color (ACI 1-255)
- `lineTypeHandle` (DwgHandleRef) - Reference to linetype
- `lineWeight` (double) - Line thickness (R2000+)

**Version Support**: All versions (R13-R2018)
- R2000+: Includes lineweight parsing
- R13/R14: Basic color and linetype

**Example**:
```java
LayerTableParser parser = new LayerTableParser();
List<DwgLayer> layers = parser.parse(section, version);

for (DwgLayer layer : layers) {
    System.out.printf("Layer: %s (frozen=%b, on=%b, color=%d)\n",
        layer.getName(), layer.isFrozen(), layer.isOn(), 
        layer.getColor().getColorIndex());
}
```

### LinetypeTableParser

**Purpose**: Extracts linetype pattern specifications.

**Section Name**: `AcDb:Linetypes`

**Return Type**: `List<Map<String, Object>>`

**Linetype Properties** (in Map):
- `name` (String) - Linetype name (e.g., "DASHED")
- `description` (String) - Human-readable description
- `flags` (int) - Linetype flags
- `dashCount` (int) - Number of dash segments
- `totalLength` (double) - Total pattern length
- `dashes` (List<Double>) - Array of dash/gap lengths

**Version Support**: All versions

**Example**:
```java
LinetypeTableParser parser = new LinetypeTableParser();
List<Map<String, Object>> linetypes = parser.parse(section, version);

for (Map<String, Object> lt : linetypes) {
    String name = (String) lt.get("name");
    List<Double> dashes = (List<Double>) lt.get("dashes");
    System.out.printf("Linetype: %s (pattern length=%f, %d segments)\n",
        name, lt.get("totalLength"), dashes.size());
}
```

### StyleTableParser

**Purpose**: Extracts text style definitions for drawing labels and annotations.

**Section Name**: `AcDb:Styles`

**Return Type**: `List<DwgStyle>`

**Style Properties**:
- `name` (String) - Style name (e.g., "Standard", "Architectural")
- `fontFilename` (String) - Primary font file (e.g., "txt.shx", "arial.ttf")
- `bigFontFilename` (String) - Big font for CJK characters (e.g., "gbcbig.shx")
- `width` (double) - Width scaling factor (0.5-2.0 typical)
- `oblique` (double) - Oblique angle in degrees (-90 to +90)
- `flags` (int) - Style flags (backward, upside-down, etc.)

**Version Support**: All versions

**Example**:
```java
StyleTableParser parser = new StyleTableParser();
List<DwgStyle> styles = parser.parse(section, version);

for (DwgStyle style : styles) {
    System.out.printf("Style: %s\n", style.getName());
    System.out.printf("  Font: %s\n", style.getFontFilename());
    System.out.printf("  Width: %.2f, Oblique: %.1f°\n", 
        style.getWidth(), style.getOblique());
}
```

## Implementation Details

### Architecture

All three parsers extend `AbstractSectionParser<T>`:

```
AbstractSectionParser
├── LayerTableParser → List<DwgLayer>
├── LinetypeTableParser → List<Map<String, Object>>
└── StyleTableParser → List<DwgStyle>
```

### Version Awareness

Parsers automatically adjust parsing logic based on DWG version:

```java
// LayerTableParser example
if (!version.until(DwgVersion.R14)) {
    // R2000+ has lineweight field
    int lineweight = reader.readBitShort();
    layer.setLineWeight(lineweight);
}
```

### Error Handling

Parsers implement graceful degradation:

```java
try {
    // Parse each entry
    DwgLayer layer = parseOneLayer(reader, version);
} catch (Exception e) {
    // Log and continue to next entry
    System.out.printf("[DEBUG] Parse error: %s\n", e.getMessage());
    break;
}
```

## Registration

Parsers are automatically registered in `SectionParserRegistry.defaultRegistry()`:

```java
public static SectionParserRegistry defaultRegistry() {
    SectionParserRegistry reg = new SectionParserRegistry();
    // ... core parsers ...
    
    // Auxiliary table parsers (Phase 4)
    reg.register(new LayerTableParser());
    reg.register(new LinetypeTableParser());
    reg.register(new StyleTableParser());
    
    return reg;
}
```

## Testing

Run the integration test:

```bash
javac -d out/classes -sourcepath src src/debug/TestAuxiliarySectionParsers.java
java -cp out/classes debug.TestAuxiliarySectionParsers
```

Output shows all 3 parsers registered and available:

```
SectionParserRegistry initialized with auxiliary parsers
✓ LayerTableParser ready
✓ LinetypeTableParser ready
✓ StyleTableParser ready
```

## Future Extensions

### Planned Parsers (Phase 5+)

- **BlockTableParser** - Block definitions
- **ViewTableParser** - Saved view configurations
- **DimStyleTableParser** - Dimension style settings
- **AppIdTableParser** - Application identifiers
- **VPortTableParser** - Viewport configurations

### Current Limitations

1. **R2000 Table Extraction**: Objects section format unclear - cannot yet extract table objects from R2000 files
2. **Table Object Locators**: Need implementation to identify Layer/Linetype/Style table objects within Objects section
3. **Real Data Testing**: Parsers tested with empty streams; need actual table section data for validation

## File Locations

```
src/io/dwg/sections/tables/
├── LayerTableParser.java
├── LinetypeTableParser.java
└── StyleTableParser.java

src/io/dwg/sections/
└── SectionParserRegistry.java (updated with registrations)

src/debug/
└── TestAuxiliarySectionParsers.java (integration test)
```

## See Also

- [Phase 4 Completion Status](PHASE4_COMPLETION_STATUS.md)
- [SectionParser Architecture](src/io/dwg/sections/SectionParser.java)
- [DWG Format Specification](doc/OpenDesign_Specification_for_.dwg_files.pdf)
